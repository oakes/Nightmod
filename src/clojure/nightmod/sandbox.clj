(ns nightmod.sandbox
  (:require [clojail.core :as jail]
            [clojail.jvm :as jvm]
            [clojail.testers :as jail-test]
            [clojure.java.io :as io]
            [nightcode.utils :as nc-utils]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [play-clj.core :refer :all])
  (:import [java.io FilePermission StringWriter]
           [java.lang.reflect ReflectPermission]))

(defn set-policy!
  []
  (System/setProperty "java.security.policy"
                      (-> "java.policy" io/resource .toString)))

(def blacklist-symbols
  '#{alter-var-root resolve find-var with-redefs-fn intern
     *read-eval* eval
     addMethod forName
     load load-file load-string load-reader
     ns-resolve ns-publics ns-unmap ns-map ns-interns the-ns in-ns require
     System/out System/in System/err
     defscreen* defgame* defgame set-screen! setScreen set-screen-wrapper!
     app! app on-gl pref! screenshot!
     asset-manager* asset-manager set-asset-manager!})

(def tester
  [(jail-test/blacklist-objects
     [clojure.lang.Compiler clojure.lang.Namespace
      clojure.lang.Ref clojure.lang.Reflector clojure.lang.RT
      java.io.ObjectInputStream java.lang.Thread])
   (jail-test/blacklist-packages
     ["java.lang.reflect"
      "java.security"
      "java.util.concurrent"
      "java.awt"])
   (jail-test/blacklist-symbols blacklist-symbols)
   (jail-test/blacklist-nses '[clojure.main])
   (jail-test/blanket "clojail")])

(def context (-> (doto (jvm/permissions)
                   (.add (FilePermission. "<<ALL FILES>>" "read"))
                   (.add (ReflectPermission. "suppressAccessChecks")))
                 jvm/domain
                 jvm/context))

(defn create-sandbox
  []
  (jail/sandbox tester
                :context context
                :timeout 5000
                :namespace u/game-ns
                :max-defs Integer/MAX_VALUE
                :init '(require '[nightmod.game :refer :all]
                                '[play-clj.core :refer :all]
                                '[play-clj.g2d :refer :all]
                                '[play-clj.g3d :refer :all]
                                '[play-clj.math :refer :all]
                                '[play-clj.physics :refer :all]
                                '[play-clj.ui :refer :all])))

(defn run-file!
  [path]
  (reset! u/error nil)
  (reset! u/out "")
  (let [writer (StringWriter.)
        sb (create-sandbox)]
    (-> (format "(do %s\n)" (slurp path))
        jail/safe-read
        (sb {#'*out* writer})
        (try
          (catch Exception e
            (reset! u/error
                    {:message (nc-utils/get-string :error-load)
                     :exception e}))
          (finally (u/set-out! (str writer)))))))

(defn run-in-sandbox!
  [func]
  (binding [*out* (StringWriter.)]
    (try
      (jvm/jvm-sandbox func context)
      (catch Exception e
        (when (nil? @u/error)
          (reset! u/error
                  {:message (some->> (:name (meta func))
                                     (format (nc-utils/get-string :error-in)))
                   :exception e}))
        nil)
      (finally (u/set-out! (str *out*))))))

; set namespaces we want to provide completions for
(intern 'nightcode.completions
        '*namespaces*
        ['clojure.core
         u/game-ns
         'play-clj.core
         'play-clj.g2d
         'play-clj.g3d
         'play-clj.math
         'play-clj.physics
         'play-clj.ui])

; set function that prevents blacklisted symbols from being in completions
(intern 'nightcode.completions
        'allow-symbol?
        (fn [symbol-str ns]
          (not (contains? blacklist-symbols (symbol symbol-str)))))

; sandbox all screen functions
(set-screen-wrapper!
  (fn [screen screen-fn]
    (if (or (= screen (:screen screens/main-screen))
            (= screen (:screen screens/overlay-screen)))
      (screen-fn)
      (run-in-sandbox! screen-fn))))

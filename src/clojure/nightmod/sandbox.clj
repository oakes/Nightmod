(ns nightmod.sandbox
  (:require [clojail.core :as jail]
            [clojail.jvm :as jvm]
            [clojail.testers :as jail-test]
            [clojure.java.io :as io]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [play-clj.core :refer :all])
  (:import [java.io FilePermission]
           [java.lang.reflect ReflectPermission]))

(def blacklist-symbols
  '#{alter-var-root resolve find-var with-redefs-fn intern
     *read-eval* set! eval catch
     addMethod forName
     load load-file load-string load-reader
     ns-resolve ns-publics ns-unmap ns-map ns-interns the-ns in-ns require
     System/out System/in System/err
     defscreen* defgame* defgame set-screen! setScreen set-screen-wrapper!
     app! app on-gl pref!
     asset-manager* asset-manager set-asset-manager!
     reify proxy gen-class})

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

(def sb (jail/sandbox tester
                      :context context
                      :timeout 5000
                      :namespace u/game-ns
                      :max-defs Integer/MAX_VALUE
                      :init '(do
                               (require '[nightmod.game :refer :all]
                                        '[play-clj.core :refer :all]
                                        '[play-clj.g2d :refer :all]
                                        '[play-clj.g3d :refer :all]
                                        '[play-clj.math :refer :all]
                                        '[play-clj.physics :refer :all]
                                        '[play-clj.ui :refer :all])
                               ; initialize box2d
                               (try (Class/forName
                                      "com.badlogic.gdx.physics.box2d.World")
                                 (catch Exception _))
                               ; initialize bullet
                               @init-bullet)))

(defn set-policy!
  []
  (System/setProperty "java.security.policy"
                      (-> "java.policy" io/resource .toString)))

(defn run-file!
  [path]
  (reset! u/error nil)
  (-> (format "(do %s\n)" (slurp path))
      jail/safe-read
      sb
      (try (catch Exception e (reset! u/error {:message "Error during load"
                                               :exception e})))))

(defn run-in-sandbox!
  [func]
  (try
    (jvm/jvm-sandbox func context)
    (catch Exception e
      (when (nil? @u/error)
        (reset! u/error {:message (some->> (:name (meta func))
                                           (format "Error in %s"))
                         :exception e}))
      nil)))

; set namespaces we want to provide completions for
(intern 'nightcode.completions
        '*namespaces*
        ['clojure.core
         'nightmod.tools
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

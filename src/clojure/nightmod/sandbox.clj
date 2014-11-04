(ns nightmod.sandbox
  (:require [clojail.core :as jail]
            [clojail.jvm :as jvm]
            [clojail.testers :as jail-test]
            [clojure.java.io :as io]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :as rt]
            [clojure.walk :refer [walk]]
            [nightcode.utils :as nc-utils]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.net])
  (:import [java.io File FilePermission StringWriter]
           [java.lang.reflect ReflectPermission]
           [java.net SocketPermission]
           [java.util.concurrent TimeoutException]))

(defn set-policy!
  []
  (System/setProperty "java.security.policy"
                      (-> "java.policy" io/resource .toString)))

(defn check-for-timeout!
  []
  (when (> (- (System/currentTimeMillis) @u/last-frame) u/timeout)
    (throw (TimeoutException. "Execution timed out."))))

(def blacklist-symbols
  '#{alter-var-root resolve find-var with-redefs-fn intern
     *read-eval* eval
     addMethod forName
     load load-file load-string load-reader
     ns-resolve ns-publics ns-unmap ns-map ns-interns the-ns in-ns
     System/out System/in System/err
     defscreen* defgame* defgame set-screen! setScreen set-screen-wrapper!
     app! app on-gl pref! screenshot!
     asset-manager* asset-manager asset-manager! set-asset-manager!})

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
   (jail-test/blacklist-nses '[clojure.main
                               nightmod.core
                               nightmod.docs
                               nightmod.input
                               nightmod.manager
                               nightmod.overlay
                               nightmod.repl
                               nightmod.sandbox
                               nightmod.screens
                               nightmod.utils
                               play-clj.net-utils
                               play-clj.utils])
   (jail-test/blanket "clojail" "nightcode")])

(def context
  (memoize (fn [path]
             (-> (doto (jvm/permissions)
                   (.add (FilePermission. "<<ALL FILES>>" "read"))
                   (.add (-> (io/file path)
                             .getCanonicalPath
                             (str File/separatorChar "*")
                             (FilePermission. "write")))
                   (.add (ReflectPermission. "suppressAccessChecks"))
                   (.add (SocketPermission. "play-clj.net" "connect")))
                 jvm/domain
                 jvm/context))))

(defn create-sandbox
  []
  (jail/sandbox tester
                :context (context @u/project-dir)
                :timeout u/timeout
                :namespace u/game-ns
                :max-defs Integer/MAX_VALUE
                :init '(require '[nightmod.game :refer :all]
                                '[play-clj.core :refer :all]
                                '[play-clj.g2d :refer :all]
                                '[play-clj.g3d :refer :all]
                                '[play-clj.math :refer :all]
                                '[play-clj.net :refer :all]
                                '[play-clj.physics :refer :all]
                                '[play-clj.ui :refer :all])))

(defn safe-read
  [f]
  (binding [*read-eval* false]
    (-> (format "(do %s\n)" (slurp f))
        (rt/indexing-push-back-reader 1 (.getName f))
        reader/read)))

(defn run-file!
  [path]
  (reset! u/error nil)
  (reset! u/out nil)
  (let [writer (StringWriter.)
        sb (create-sandbox)]
    (-> (io/file path)
        safe-read
        (sb {#'*out* writer})
        (try
          (catch Exception e
            (when-not @u/error
              (reset! u/error
                      {:message (nc-utils/get-string :error-load)
                       :exception e}))
            (on-gl (screens/set-blank-screen!)))
          (finally (u/set-out! (str writer) true))))))

(defn run-in-sandbox!
  [func]
  (binding [*out* (StringWriter.)]
    (try
      (jvm/jvm-sandbox func (context @u/project-dir))
      (catch Exception e
        (when-not @u/error
          (reset! u/error
                  {:message (some->> (:name (meta func))
                                     (format (nc-utils/get-string :error-in)))
                   :exception e}))
        (screens/set-blank-screen!))
      (finally (u/set-out! (str *out*) false)))))

; set namespaces we want to provide completions for
(intern 'nightcode.completions
        '*namespaces*
        ['clojure.core
         'nightmod.game
         'play-clj.core
         'play-clj.g2d
         'play-clj.g3d
         'play-clj.math
         'play-clj.net
         'play-clj.physics
         'play-clj.ui])

; set function that prevents blacklisted symbols from being in completions
(intern 'nightcode.completions
        'allow-symbol?
        (fn [symbol-str ns]
          (not (contains? blacklist-symbols (symbol symbol-str)))))

; replace clojail's ensafen with a custom version that dotifies before
; macroexpanding, and adds a timeout checker to all recur calls
(intern 'clojail.core
        'ensafen
        (-> (fn macroexpand-most [form]
              (if (or (not (coll? form))
                      (and (seq? form)
                           (= 'quote (first form))))
                form
                (let [form (walk macroexpand-most identity (macroexpand form))]
                  (if (and (seq? form) (= 'recur (first form)))
                    (list 'do '(nightmod.sandbox/check-for-timeout!) form)
                    form))))
            (comp #'clojail.core/dotify)))

; sandbox all screen functions
(set-screen-wrapper!
  (fn [screen screen-fn]
    (reset! u/last-frame (System/currentTimeMillis))
    (if (or (= screen (:screen screens/main-screen))
            (= screen (:screen screens/overlay-screen)))
      (screen-fn)
      (run-in-sandbox! screen-fn))))

; use the play-clj.net server
(intern 'play-clj.net-utils 'client-send-address "tcp://play-clj.net:4707")
(intern 'play-clj.net-utils 'client-receive-address "tcp://play-clj.net:4708")

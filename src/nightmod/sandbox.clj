(ns nightmod.sandbox
  (:require [clojail.core :as jail]
            [clojail.jvm :as jvm]
            [clojail.testers :as jail-test]
            [clojure.java.io :as io]
            [nightmod.screens :as s]
            [nightmod.utils :as u]
            [play-clj.core :refer :all])
  (:import [java.io FilePermission]
           [java.lang.reflect ReflectPermission]))

(def manager (asset-manager))

(def blacklist-symbols
  '#{alter-var-root resolve find-var with-redefs-fn intern
     *read-eval* set! eval catch
     addMethod forName
     load load-file load-string load-reader
     ns-resolve ns-publics ns-unmap ns-map ns-interns the-ns in-ns
     push-thread-bindings pop-thread-bindings future future-call
     agent send send-off
     pmap pvalues pcalls
     System/out System/in System/err
     defscreen* defgame* defgame set-screen! setScreen set-screen-wrapper!
     app! app on-gl
     loader loader! resolver
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

(def game-ns 'nightmod.game)

(def sb (jail/sandbox tester
                      :context context
                      :timeout 5000
                      :namespace game-ns
                      :max-defs Integer/MAX_VALUE
                      :init '(do
                               (require '[nightmod.public :refer :all]
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

(defn clear-ns!
  [nspace]
  (doall (map #(ns-unmap nspace %) (keys (ns-interns nspace)))))

(defn run-file!
  [path]
  (reset! u/error nil)
  (clear-ns! game-ns)
  (on-gl (asset-manager! manager :clear))
  (-> (format "(do %s\n)" (slurp path))
      jail/safe-read
      sb
      (try (catch Exception e (reset! u/error e)))))

(intern 'nightcode.completions
        '*namespaces*
        ['clojure.core
         'nightmod.public
         'play-clj.core
         'play-clj.g2d
         'play-clj.g3d
         'play-clj.math
         'play-clj.physics
         'play-clj.ui])

(intern 'nightcode.completions
        'allow-symbol?
        (fn [symbol-str ns]
          (not (contains? blacklist-symbols (symbol symbol-str)))))

(set-screen-wrapper!
  (fn [screen screen-fn]
    (if (or (= screen (:screen s/main-screen))
            (= screen (:screen s/overlay-screen)))
      (screen-fn)
      (try
        (jvm/jvm-sandbox screen-fn context)
        (catch Exception e
          (when (nil? @u/error)
            (reset! u/error e))
          nil)))))

(set-asset-manager! manager)

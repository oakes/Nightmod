(ns nightmod.sandbox
  (:require [clojail.core :as jail]
            [clojail.jvm :as jvm]
            [clojail.testers :as jail-test]
            [clojure.java.io :as io]
            [nightmod.utils :as u]))

(def tester
  [(jail-test/blacklist-objects
     [clojure.lang.Compiler clojure.lang.Ref clojure.lang.Reflector
      clojure.lang.Namespace clojure.lang.RT
      java.io.ObjectInputStream])
   (jail-test/blacklist-packages
     ["java.lang.reflect"
      "java.security"
      "java.util.concurrent"
      "java.awt"])
   (jail-test/blacklist-symbols
    '#{alter-var-root eval catch 
       load-string load-reader addMethod ns-resolve resolve find-var
       *read-eval* ns-publics ns-unmap set! ns-map ns-interns the-ns
       push-thread-bindings pop-thread-bindings future-call agent send
       send-off pmap pcalls pvals in-ns System/out System/in System/err
       with-redefs-fn Class/forName
       set-screen! setScreen app! postRunnable})
   (jail-test/blacklist-nses '[clojure.main])
   (jail-test/blanket "clojail")])

(def context (-> (doto (jvm/permissions)
                   (.add (java.io.FilePermission. "<<ALL FILES>>" "read")))
                 jvm/domain
                 jvm/context))

(def sb (jail/sandbox tester
                      :context context
                      :timeout 5000
                      :namespace 'nightmod.game
                      :init '(require '[nightmod.public :refer :all]
                                      '[play-clj.core :refer :all]
                                      '[play-clj.g2d :refer :all]
                                      '[play-clj.g2d-physics :refer :all]
                                      '[play-clj.g3d :refer :all]
                                      '[play-clj.math :refer :all]
                                      '[play-clj.ui :refer :all]
                                      '[play-clj.utils :refer :all])))

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
      (try (catch Exception e (reset! u/error e)))))

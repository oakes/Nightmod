(ns nightmod.core.sandbox
  (:require [clojail.core :as jail]
            [clojail.testers :as jail-test]
            [clojure.java.io :as io]))

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
       with-redefs-fn Class/forName})
   (jail-test/blacklist-nses '[clojure.main])
   (jail-test/blanket "clojail")])

(def sb (jail/sandbox tester
                      :timeout 5000
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

(defn run!
  [path]
  (->> path
       slurp
       (format "(do %s\n)")
       jail/safe-read
       sb))

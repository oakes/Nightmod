(ns nightmod.sandbox
  (:require [clojure.java.io :as io]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :as rt]
            [clojure.walk :refer [walk]]
            [nightcode.utils :as nc-utils]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [play-clj.core :as c])
  (:import [java.io File StringWriter]))

(defn require-nses []
  (require '[nightmod.game :refer :all]
           '[play-clj.core :refer :all]
           '[play-clj.g2d :refer :all]
           '[play-clj.g3d :refer :all]
           '[play-clj.math :refer :all]
           '[play-clj.net :refer :all]
           '[play-clj.physics :refer :all]
           '[play-clj.ui :refer :all]))

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
  (let [writer (StringWriter.)]
    (try
      (require-nses)
      (-> path io/file safe-read eval)
      (catch Throwable e
        (when-not @u/error
          (reset! u/error
                  {:message (nc-utils/get-string :error-load)
                   :exception e}))
        (c/on-gl (screens/set-blank-screen!)))
      (finally (u/set-out! (str writer) true)))))


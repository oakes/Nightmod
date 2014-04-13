(ns nightmod.utils
  (:require [clojure.java.io :as io]
            [nightcode.ui :as ui]
            [seesaw.core :as s])
  (:import [java.text SimpleDateFormat]))

(def ^:const window-width 1200)
(def ^:const window-height 768)
(def ^:const editor-width 700)
(def ^:const properties-file ".properties")

(def main-dir (atom nil))
(def project-dir (atom nil))
(def error (atom nil))

(defn get-data-dir
  []
  (.getCanonicalPath (io/file (System/getProperty "user.home") "nightmod")))

(defn format-date
  [unix-time]
  (.format (SimpleDateFormat. "yyyy.MM.dd HH:mm:ss") unix-time))

(defn new-project!
  [template]
  (let [project-name (str (System/currentTimeMillis))
        project-file (io/file @main-dir project-name)]
    (.mkdirs project-file)
    (doseq [f (-> (io/resource template) io/file .listFiles)]
      (io/copy f (io/file project-file (.getName f))))
    (.getCanonicalPath project-file)))

(defn glass
  []
  (.getGlassPane @ui/root))

(defn toggle-glass!
  ([]
    (-> (glass) .isVisible not toggle-glass!))
  ([show?]
    (s/invoke-now
      (.setVisible (glass) show?)
      (.revalidate @ui/root))))

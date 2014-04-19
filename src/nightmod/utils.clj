(ns nightmod.utils
  (:require [clojure.java.io :as io]
            [nightcode.editors :as editors]
            [nightcode.ui :as ui]
            [seesaw.core :as s])
  (:import [java.text SimpleDateFormat]
           [java.awt Robot]
           [java.awt.event InputEvent]
           [javax.swing SwingUtilities]))

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

(defn focus-on-text!
  []
  (some-> (editors/get-selected-text-area) s/request-focus!))

(defn glass
  []
  (.getGlassPane @ui/root))

(defn toggle-glass!
  ([]
    (-> (glass) .isVisible not toggle-glass!))
  ([show?]
    (s/invoke-now
      (.setVisible (glass) show?)
      (.revalidate @ui/root)
      ; this is a hack to force the editor pane into focus
      (when show?
        (let [pane (s/select @ui/root [:#editor-pane])
              pane-pos (.getLocationOnScreen pane)
              mouse-pos (.getMousePosition @ui/root)]
          (SwingUtilities/convertPointToScreen mouse-pos @ui/root)
          (doto (Robot.)
            (.mouseMove (. pane-pos x) (. pane-pos y))
            (.mousePress InputEvent/BUTTON1_MASK)
            (.mouseRelease InputEvent/BUTTON1_MASK)
            (.mouseMove (. mouse-pos x) (. mouse-pos y))))))))

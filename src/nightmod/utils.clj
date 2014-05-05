(ns nightmod.utils
  (:require [clojure.java.io :as io]
            [nightcode.editors :as editors]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [seesaw.core :as s])
  (:import [java.text SimpleDateFormat]
           [java.awt Robot]
           [java.awt.event InputEvent]
           [javax.swing SwingUtilities]))

(def ^:const window-width 1200)
(def ^:const window-height 768)
(def ^:const editor-width 700)
(def ^:const settings-file "settings.edn")

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
    (doseq [f (-> template io/resource io/file .listFiles)]
      (io/copy f (io/file project-file (.getName f))))
    (spit (io/file project-file settings-file)
          (format (-> settings-file io/resource slurp)
                  (nc-utils/get-string template)))
    (.getCanonicalPath project-file)))

(defn glass
  []
  (.getGlassPane @ui/root))

(defn focus-on-overlay!
  []
  (when (.isVisible (glass))
    (s/invoke-now
      (when-let [pane (s/select @ui/root [:#editor-pane])]
        (let [pane-pos (.getLocationOnScreen pane)
              mouse-pos (.getMousePosition @ui/root)]
          (when (and pane-pos mouse-pos)
            (SwingUtilities/convertPointToScreen mouse-pos @ui/root)
            (doto (Robot.)
              (.mouseMove
                (+ (. pane-pos x) (- (.getWidth pane) 5))
                (+ (. pane-pos y) 5))
              (.mousePress InputEvent/BUTTON1_MASK)
              (.mouseRelease InputEvent/BUTTON1_MASK)
              (.mouseMove (. mouse-pos x) (. mouse-pos y))))))
      (some-> (editors/get-selected-text-area) s/request-focus!))))

(defn toggle-glass!
  ([]
    (-> (glass) .isVisible not toggle-glass!))
  ([show?]
    (s/invoke-now
      (.setVisible (glass) show?)
      (.revalidate @ui/root))
    (focus-on-overlay!)))

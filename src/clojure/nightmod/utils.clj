(ns nightmod.utils
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [nightcode.dialogs :as dialogs]
            [nightcode.editors :as editors]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [seesaw.core :as s])
  (:import [java.text SimpleDateFormat]))

(def ^:const window-width 1200)
(def ^:const window-height 768)
(def ^:const editor-width 700)
(def ^:const settings-file "settings.edn")
(def templates ["arcade"
                "platformer"
                "orthogonal-rpg"
                "isometric-rpg"
                "barebones-2d"
                "barebones-3d"])
(def template-files {"arcade" ["core.clj"
                               "enemy.png"
                               "entities.clj"
                               "player.png"]
                     "platformer" ["core.clj"]
                     "orthogonal-rpg" ["core.clj"]
                     "isometric-rpg" ["core.clj"]
                     "barebones-2d" ["core.clj"]
                     "barebones-3d" ["core.clj"]})

(def main-dir (atom nil))
(def project-dir (atom nil))
(def error (atom nil))

(defn get-data-dir
  []
  (.getCanonicalPath (io/file (System/getProperty "user.home") "nightmod")))

(defn format-date
  [unix-time]
  (.format (SimpleDateFormat. "yyyy.MM.dd HH:mm:ss") unix-time))

(defn format-project-dir
  [project-name]
  (-> project-name
      (string/replace " " "-")
      nc-utils/format-project-name))

(defn new-project-name!
  [template]
  (s/invoke-now
    (dialogs/show-text-field-dialog!
      (nc-utils/get-string :enter-project-name)
      (nc-utils/get-string template))))

(defn new-project-dir!
  [project-name]
  (let [dir-name (format-project-dir project-name)
        project-file (io/file @main-dir dir-name)]
    (cond
      (= 0 (count dir-name))
      (s/invoke-now
        (dialogs/show-simple-dialog! (nc-utils/get-string :invalid-name)))
      (.exists project-file)
      (s/invoke-now
        (dialogs/show-simple-dialog! (nc-utils/get-string :file-exists)))
      :else
      project-file)))

(defn new-project!
  [template project-name project-file]
  (.mkdirs project-file)
  (doseq [file-name (get template-files template)]
    (-> (str template "/" file-name)
        io/resource
        io/input-stream
        (io/copy (io/file project-file file-name))))
  (->> (format (slurp (io/resource settings-file)) project-name)
       (spit (io/file project-file settings-file)))
  (.getCanonicalPath project-file))

(defn glass
  []
  (.getGlassPane @ui/root))

(defn focus-on-overlay!
  []
  (s/invoke-now
    (.grabFocus (or (editors/get-selected-text-area)
                    (s/select @ui/root [:#editor-pane])
                    (glass)))))

(defn toggle-glass!
  ([]
    (-> (glass) .isVisible not toggle-glass!))
  ([show?]
    (s/invoke-now
      (.setVisible (glass) show?)
      (.revalidate @ui/root)
      (if show?
        (focus-on-overlay!)
        (s/request-focus! @ui/root)))))

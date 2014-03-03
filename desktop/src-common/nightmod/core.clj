(ns nightmod.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]))

(declare nightmod main-screen)

(def ^:const templates ["arcade" "platformer"
                        "orthogonal-rpg" "isometric-rpg"
                        "barebones-2d" "barebones-3d"])

(defn read-title
  [f]
  [(.getCanonicalPath f)
   (or (-> (io/file f u/properties-file)
            slurp
            edn/read-string
            :title
            (try (catch Exception _)))
       (-> (.getName f)
           Long/parseLong
           u/format-date
           (try (catch Exception _)))
       "Invalid")])

(defn load-project!
  [path]
  (reset! u/project-dir path)
  (println "Load Project:" path))

(defn new-project!
  [template]
  (->> (System/currentTimeMillis)
       str
       (io/file @u/main-dir u/projects-dir)
       .getCanonicalPath
       (u/apply-template template)
       load-project!))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage) :camera (orthographic))
    (when-not @u/main-dir
      (reset! u/main-dir (u/get-data-dir)))
    (let [ui-skin (skin "uiskin.json")
          projects-dir (doto (io/file @u/main-dir u/projects-dir) .mkdirs)
          create-button (fn [[k v]]
                          (text-button v ui-skin :set-name k))
          template-names ["Arcade" "Platformer"
                          "Orthogonal RPG" "Isometric RPG"
                          "Barebones 2D" "Barebones 3D"]
          new-games (->> (for [i (range (count templates))]
                           [(nth templates i)
                            (nth template-names i)])
                         (map create-button))
          saved-games (->> (.listFiles projects-dir)
                           (filter #(.isDirectory %))
                           (map read-title)
                           (map create-button))]
      (-> (cons (label "New Game:" ui-skin) new-games)
          (concat (when (seq saved-games)
                    (cons (label "Load Game:" ui-skin) saved-games)))
          vertical
          (scroll-pane (style :scroll-pane nil nil nil nil nil))
          list
          (table :align (align :center) :set-fill-parent true))))
  :on-render
  (fn [screen entities]
    (clear! 0 0 0 0)
    (render! screen entities))
  :on-resize
  (fn [screen entities]
    (height! screen 400))
  :on-ui-changed
  (fn [screen entities]
    (when-let [n (-> screen :actor .getName)]
      (if (contains? (set templates) n)
        (new-project! n)
        (load-project! n)))
    nil))

(defgame nightmod
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

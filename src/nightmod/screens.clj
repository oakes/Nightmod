(ns nightmod.screens
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]))

(def ^:const screen-height 350)
(def ^:const line-height 20)
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
  (reset! u/project-dir path))

(defn new-project!
  [template]
  (load-project! (u/new-project! template)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage) :camera (orthographic))
    (when-not @u/main-dir
      (reset! u/main-dir (u/get-data-dir)))
    (let [ui-skin (skin "uiskin.json")
          create-button (fn [[k v]]
                          (text-button v ui-skin :set-name k))
          template-names ["Arcade" "Platformer"
                          "Orthogonal RPG" "Isometric RPG"
                          "Barebones 2D" "Barebones 3D"]
          new-games (->> (for [i (range (count templates))]
                           [(nth templates i)
                            (nth template-names i)])
                         (map create-button))
          saved-games (->> (io/file @u/main-dir)
                           .listFiles
                           (filter #(.isDirectory %))
                           (map read-title)
                           (map create-button))]
      (-> (cons (label "New Game:" ui-skin) new-games)
          (concat (when (seq saved-games)
                    (cons (label "Load Game:" ui-skin) saved-games)))
          (vertical :pack)
          (scroll-pane (style :scroll-pane nil nil nil nil nil))
          list
          (table :align (align :center) :set-fill-parent true))))
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities))
  :on-resize
  (fn [screen entities]
    (height! screen (:height screen)))
  :on-ui-changed
  (fn [screen entities]
    (when-let [n (-> screen :actor .getName)]
      (if (contains? (set templates) n)
        (new-project! n)
        (load-project! n)))
    nil))

(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(defscreen overlay-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    [(assoc (label "" (color :white))
            :id :fps
            :x 5
            :y (- screen-height line-height))
     (assoc (label "" (color :white) :set-wrap true)
            :id :error
            :x 5)])
  :on-render
  (fn [screen entities]
    (->> (for [e entities]
           (case (:id e)
             :fps (doto e
                    (label! :set-text (str (game :fps)))
                    (label! :pack))
             :error (doto e
                      (label! :set-text (or (some-> @u/error .toString) ""))
                      (label! :pack))
             e))
         (render! screen)))
  :on-resize
  (fn [screen entities]
    (height! screen screen-height)
    (for [e entities]
      (case (:id e)
        :error (assoc e :width (width screen))
        e))))

(defgame nightmod
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

(add-watch u/error
           :show-error
           (fn [_ _ _ e]
             (when e
               (->> (set-screen! nightmod blank-screen overlay-screen)
                    (fn [])
                    (app! :post-runnable)))))

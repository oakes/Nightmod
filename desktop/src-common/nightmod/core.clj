(ns nightmod.core
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage) :camera (orthographic))
    (let [ui-skin (skin "uiskin.json")
          create-button (fn [[k v]]
                          (text-button v ui-skin :set-name k))
          templates [["arcade" "Arcade"]
                     ["platformer" "Platformer"]
                     ["orthogonal-rpg" "Orthogonal RPG"]
                     ["isometric-rpg" "Isometric RPG"]
                     ["barebones-2d" "Barebones 2D"]
                     ["barebones-3d" "Barebones 3D"]]
          new-games (map create-button templates)
          saved-games (map create-button [])]
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
    (println (:actor screen))))

(defgame nightmod
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

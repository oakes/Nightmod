(ns nightmod.core
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage) :camera (orthographic))
    (let [ui-skin (skin "uiskin.json")
          saved-games (map #(text-button % ui-skin) [])]
      (-> [(label "New Game:" ui-skin)
           (text-button "Arcade" ui-skin)
           (text-button "Platformer" ui-skin)
           (text-button "Orthogonal RPG" ui-skin)
           (text-button "Isometric RPG" ui-skin)
           (text-button "Barebones 2D" ui-skin)
           (text-button "Barebones 3D" ui-skin)]
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

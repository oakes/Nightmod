(set! *warn-on-reflection* true)

(ns nightmod.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage) :camera (orthographic))
    entities)
  :on-render
  (fn [screen entities]
    (clear! 0 0 0 0)
    (render! screen entities)))

(defgame nightmod
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

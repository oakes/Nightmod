(set! *warn-on-reflection* true)

(ns nightmod.core
  (:require [play-clj.core :refer :all]
            [play-clj.g3d :refer :all]
            [play-clj.math :as m]))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :renderer (model-batch)
             :attributes (let [attr-type (attribute-type :color :ambient-light)
                               attr (attribute :color attr-type 0.8 0.8 0.8 1)]
                           (environment :set attr))
             :camera (doto (perspective 75 (game :width) (game :height))
                       (position! 0 0 3)
                       (direction! 0 0 0)
                       (near! 0.1)
                       (far! 300)))
    (let [attr (attribute! :color :create-diffuse (color :blue))
          model-mat (material :set attr)
          model-attrs (bit-or (usage :position) (usage :normal))
          builder (model-builder)]
      (model (model-builder! builder :create-box 2 2 2 model-mat model-attrs)
             (m/vector-3 0 0 0))))
  :on-render
  (fn [screen entities]
    (clear! 1 1 1 1)
    (doto screen
      (perspective! :rotate-around (m/vector-3 0 0 0) (m/vector-3 0 1 0) 1)
      (perspective! :update))
    (render! screen entities)))

(defgame nightmod
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

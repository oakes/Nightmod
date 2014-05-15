(load-game-file "entities.clj")

(defn update-screen!
  [screen entities]
  (doseq [entity entities]
    (when (:player? entity)
      (x! screen (:x entity))
      (when (< (:y entity) (- (:height entity)))
        (restart-game!))))
  entities)

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (->> (orthogonal-tiled-map "level.tmx" (/ 1 16))
         (update! screen :camera (orthographic) :renderer))
    (create-player))
  
  :on-render
  (fn [screen entities]
    (clear! 0.5 0.5 1 1)
    (->> entities
         (map (fn [entity]
                (->> entity
                     (move screen)
                     (prevent-move screen)
                     (animate screen))))
         (render! screen)
         (update-screen! screen)))
  
  :on-resize
  (fn [screen entities]
    (orthographic! screen
                   :set-to-ortho
                   false
                   (* vertical-tiles (/ (:width screen) (:height screen)))
                   vertical-tiles)))

(set-game-screen! main-screen)

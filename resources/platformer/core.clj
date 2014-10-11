(load-game-file "entities.clj")

(defn update-screen!
  [screen entities]
  (doseq [entity entities]
    (when (:player? entity)
      (position! screen (:x entity) (/ vertical-tiles 2))
      (when (< (:y entity) (- (:height entity)))
        (restart-game!))))
  entities)

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :camera (orthographic)
             :renderer (orthogonal-tiled-map "level.tmx" 1/16))
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
    (height! screen vertical-tiles)))

(set-game-screen! main-screen)

(load-game-file "entities.clj")

(defn update-screen!
  [screen entities]
  (doseq [{:keys [x y player?]} entities]
    (when player?
      (position! screen x y)))
  entities)

(defn play-sounds!
  [entities]
  (doseq [{:keys [play-sound]} entities]
    (when play-sound
      (sound! play-sound :play)))
  (map #(dissoc % :play-sound) entities))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (let [screen (->> (/ 1 pixels-per-tile)
                      (orthogonal-tiled-map "level.tmx")
                      (update! screen :camera (orthographic) :renderer))
          enemies (take 10 (repeatedly #(create-enemy)))]
      (reduce #(randomize-locations screen %1 %2)
              []
              (flatten [(create-player) enemies]))))
  
  :on-render
  (fn [screen entities]
    (clear!)
    (->> entities
         (map (fn [entity]
                (->> entity
                     (move screen entities)
                     (animate screen)
                     (prevent-move screen entities)
                     (adjust-times screen))))
         (attack-player)
         (play-sounds!)
         (remove #(<= (:health %) 0))
         (sort-by :y #(compare %2 %1))
         (render! screen)
         (update-screen! screen)))
  
  :on-resize
  (fn [screen entities]
    (height! screen vertical-tiles))
  
  :on-key-down
  (fn [screen entities]
    (when-let [player (get-player entities)]
      (cond
        (= (:key screen) (key-code :space))
        (attack entities player))))
  
  :on-touch-down
  (fn [screen entities]
    (when-let [player (get-player entities)]
      (let [min-x (/ (game :width) 3)
            max-x (* (game :width) (/ 2 3))
            min-y (/ (game :height) 3)
            max-y (* (game :height) (/ 2 3))]
        (cond
          (and (< min-x (:input-x screen) max-x)
               (< min-y (:input-y screen) max-y))
          (attack entities player))))))

(set-game-screen! main-screen)

(load-game-file "entities.clj")

(print "Press the arrow keys or left click to move."
       \newline
       "Right click on enemies to attack.")

(declare main-screen npc-health-screen player-health-screen)

(defn update-screen!
  [screen entities]
  (doseq [entity entities]
    (when (:player? entity)
      (position! screen (:x entity) (:y entity))))
  entities)

(defn play-sounds!
  [entities]
  (doseq [entity entities]
    (when (:play-sound entity)
      (sound! (:play-sound entity) :play)))
  (map #(dissoc % :play-sound) entities))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (let [screen (->> (/ 1 pixels-per-tile)
                      (isometric-tiled-map "level.tmx")
                      (update! screen
                               :camera (orthographic)
                               :renderer))]
      (->> [(create-player)
            (take 8 (repeatedly #(create-enemy)))]
           (flatten)
           (reduce #(randomize-locations screen %1 %2) []))))

  :on-render
  (fn [screen entities]
    (clear!)
    (let [me (find-first :player? entities)]
      ; update health bars
      (->> (get-entity-at-cursor screen entities (game :x) (game :y))
           (run! npc-health-screen :on-update-health-bar :entity))
      (run! player-health-screen :on-update-health-bar :entity me)
      ; run game logic
      (->> entities
           (map (fn [entity]
                  (->> entity
                       (move screen entities)
                       (animate screen)
                       (prevent-move screen entities)
                       (adjust screen))))
           (attack screen (some #(if (can-attack? % me) %) entities) me)
           (play-sounds!)
           (render! screen)
           (render-sorted! screen ["things"])
           (update-screen! screen))))

  :on-resize
  (fn [screen entities]
    (height! screen vertical-tiles))

  :on-touch-down
  (fn [screen entities]
    (when (= (:button screen) (button-code :right))
      (let [me (find-first :player? entities)
            x (:input-x screen)
            y (:input-y screen)
            victim (get-entity-at-cursor screen entities x y)
            victim (when (can-attack? me victim) victim)]
        (print " ")
        (attack screen me victim entities)))))

(defscreen npc-health-screen
  :on-show
  (fn [screen entities]
    (shape :filled))
  
  :on-render
  (fn [screen entities]
    (draw! (-> main-screen :screen deref) entities))
  
  :on-update-health-bar
  (fn [screen entities]
    (if-let [e (:entity screen)]
      (let [bar (first entities)
            bar-x (:x e)
            bar-y (+ (:y e) (:height e))
            bar-w (:width e)
            pct (/ (:health e) (+ (:health e) (:wounds e)))]
        (shape bar
               :set-color (color :red)
               :rect bar-x bar-y bar-w npc-bar-h
               :set-color (color :green)
               :rect bar-x bar-y (* bar-w pct) npc-bar-h))
      (shape (first entities)))))

(defscreen player-health-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (assoc (shape :filled)
           :id :bar
           :x 5
           :y 5))
  
  :on-render
  (fn [screen entities]
    (render! screen entities))

  :on-resize
  (fn [screen entities]
    (height! screen 300))
  
  :on-update-health-bar
  (fn [screen entities]
    (let [bar (first entities)
          player (:entity screen)
          pct (/ (:health player) (+ (:health player) (:wounds player)))]
      (shape bar
             :set-color (color :red)
             :rect 0 0 bar-w bar-h
             :set-color (color :green)
             :rect 0 0 bar-w (* bar-h pct)))))

(set-game-screen! main-screen npc-health-screen player-health-screen)

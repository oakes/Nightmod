(load-game-file "entities.clj")

(print "Press the arrow keys or left click to move."
       \newline
       "Right click on enemies to attack.")

(declare main-screen player-health-screen)

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

(defn render-everything!
  [screen entities]
  (render-map! screen :with "grass")
  (->> (get-entity-at-cursor screen entities)
       (update-health-bar (:npc-health-bar screen))
       (conj entities)
       (render-sorted! screen ["things"]))
  entities)

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (let [screen (->> (/ 1 pixels-per-tile)
                      (isometric-tiled-map "level.tmx")
                      (update! screen
                               :camera (orthographic)
                               :npc-health-bar (shape :filled)
                               :renderer))]
      (->> [(create-player)
            (take 8 (repeatedly #(create-enemy)))]
           (flatten)
           (reduce #(randomize-locations screen %1 %2) []))))

  :on-render
  (fn [screen entities]
    (clear!)
    (let [me (find-first :player? entities)]
      (screen! player-health-screen :on-update-health-bar :entity me)
      (->> entities
           (map (fn [entity]
                  (->> entity
                       (move screen entities)
                       (animate screen)
                       (prevent-move screen entities)
                       (adjust screen))))
           (attack screen (find-first #(can-attack? % me) entities) me)
           (play-sounds!)
           (render-everything! screen)
           (update-screen! screen))))

  :on-resize
  (fn [screen entities]
    (height! screen vertical-tiles))

  :on-touch-down
  (fn [screen entities]
    (when (= (:button screen) (button-code :right))
      (let [me (find-first :player? entities)
            victim (get-entity-at-cursor screen entities)
            victim (when (can-attack? me victim) victim)]
        (print " ")
        (attack screen me victim entities)))))

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
             :rect 0 20 bar-w bar-h
             :set-color (color :green)
             :rect 0 20 bar-w (* bar-h pct)))))

(set-game-screen! main-screen player-health-screen)

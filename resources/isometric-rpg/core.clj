(load-game-file "entities.clj")

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
                               :attack-cursor (pixmap "glove.png")
                               :camera (orthographic)
                               :renderer))
          hurt-sound-1 (sound "player_hurt.wav")
          hurt-sound-2 (sound "enemy_hurt.wav")
          death-sound (sound "player_death.wav")
          me (assoc (create-player)
                    :x 0
                    :y 0
                    :hurt-sound hurt-sound-1
                    :death-sound death-sound)]
      (->> [(isometric->screen screen me)
            (take 10 (repeatedly #(create-enemy)))]
           flatten
           (map #(if-not (:hurt-sound %) (assoc % :hurt-sound hurt-sound-2) %))
           (reduce #(randomize-locations screen %1 %2) []))))

  :on-render
  (fn [screen entities]
    (clear!)
    (let [me (get-player entities)]
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
           (render-sorted! screen sort-entities ["things"])
           (update-screen! screen))))

  :on-resize
  (fn [screen entities]
    (height! screen vertical-tiles))

  :on-touch-down
  (fn [screen entities]
    (when (= (:button screen) (button-code :right))
      (let [me (get-player entities)
            x (:input-x screen)
            y (:input-y screen)
            victim (get-entity-at-cursor screen entities x y)
            victim (when (can-attack? me victim) victim)]
        (attack screen me victim entities))))

  :on-mouse-moved
  (fn [{:keys [input-x input-y] :as screen} entities]
    (if (get-entity-at-cursor screen entities input-x input-y)
      (input! :set-cursor-image (:attack-cursor screen) 0 0)
      (input! :set-cursor-image nil 0 0))))

(defscreen npc-health-screen
  :on-show
  (fn [screen entities]
    (shape :filled))

  :on-render
  (fn [screen entities]
    (when-let [e (get-entity-at-cursor (-> main-screen :screen deref)
                                         (-> main-screen :entities deref)
                                         (game :x)
                                         (game :y))]
      (let [bar-x (:x e)
            bar-y (+ (:y e) (:height e))
            bar-w (:width e)
            pct (/ (:health e) (+ (:health e) (:wounds e)))]
        (->> (shape (first entities)
                    :set-color (color :red)
                    :rect bar-x bar-y bar-w npc-bar-h
                    :set-color (color :green)
                    :rect bar-x bar-y (* bar-w pct) npc-bar-h)
             vector
             (draw! (-> main-screen :screen deref)))))))

(defscreen overlay-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    [(assoc (shape :filled)
            :id :bar
            :x 5
            :y 5)])

  :on-render
  (fn [screen entities]
    (->> (for [entity entities]
           (case (:id entity)
             :bar (let [me (get-player (-> main-screen :entities deref))
                        pct (/ (:health me) (+ (:health me) (:wounds me)))]
                    (shape entity
                           :set-color (color :red)
                           :rect 0 0 bar-w bar-h
                           :set-color (color :green)
                           :rect 0 0 bar-w (* bar-h pct)))
             entity))
         (render! screen)))

  :on-resize
  (fn [screen entities]
    (height! screen 300)))

(set-game-screen! main-screen npc-health-screen overlay-screen)

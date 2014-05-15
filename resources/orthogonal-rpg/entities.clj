(load-game-file "utils.clj")

(defn create
  [start-layer img]
  (assoc img
         :width 1
         :height 1.5
         :x-velocity 0
         :y-velocity 0
         :start-layer start-layer
         :min-distance 2
         :health 6
         :direction :down))

(defn create-character
  [start-layer down up stand-left walk-left]
  (let [down-flip (texture down :flip true false)
        up-flip (texture up :flip true false)
        stand-flip (texture stand-left :flip true false)
        walk-flip (texture walk-left :flip true false)]
    (assoc (create start-layer down)
           :down (animation duration [down down-flip])
           :up (animation duration [up up-flip])
           :left (animation duration [stand-left walk-left])
           :right (animation duration [stand-flip walk-flip])
           :min-distance 10
           :health 10
           :damage 4
           :attack-time 0)))

(defn create-tree
  [img]
  (assoc (create "grass" img)
         :tree? true))

(defn create-enemy
  []
  (let [down (texture "enemy_down.png")
        up (texture "enemy_up.png")
        stand-left (texture "enemy_left_stand.png")
        walk-left (texture "enemy_left_walk.png")]
    (assoc (create-character "grass" down up stand-left walk-left)
           :npc? true)))

(defn create-player
  []
  (let [down (texture "player_down.png")
        up (texture "player_up.png")
        stand-left (texture "player_left_stand.png")
        walk-left (texture "player_left_walk.png")]
    (assoc (create-character "grass" down up stand-left walk-left)
           :player? true)))

(defn move
  [screen entities entity]
  (let [[x-velocity y-velocity] (get-velocity entities entity)
        x-change (* x-velocity (:delta-time screen))
        y-change (* y-velocity (:delta-time screen))]
    (if (or (not= 0 x-change) (not= 0 y-change))
      (assoc entity
             :x-velocity (decelerate x-velocity)
             :y-velocity (decelerate y-velocity)
             :x-change x-change
             :y-change y-change
             :x (+ (:x entity) x-change)
             :y (+ (:y entity) y-change))
      entity)))

(defn animate-direction
  [screen entity]
  (if-let [direction (get-direction entity)]
    (if-let [anim (get entity direction)]
      (merge entity
             (animation->texture screen anim)
             {:direction direction})
      entity)
    entity))

(defn animate-water
  [screen entity]
  (if (completely-on-layer? screen entity "water")
    (merge entity (texture entity :set-region-height pixels-per-tile))
    entity))

(defn update-texture-size
  [entity]
  (assoc entity
         :width (/ (texture! entity :get-region-width) pixels-per-tile)
         :height (/ (texture! entity :get-region-height) pixels-per-tile)))

(defn animate
  [screen entity]
  (->> entity
       (animate-direction screen)
       (animate-water screen)
       update-texture-size))

(defn not-victim?
  [attacker victim]
  (or (= (:health attacker) 0)
      (not= (:npc? attacker) (:player? victim))
      (not (near-entity? attacker victim attack-distance))
      (case (:direction attacker)
        :down (< (- (:y attacker) (:y victim)) 0) ; victim is up?
        :up (> (- (:y attacker) (:y victim)) 0) ; victim is down?
        :right (> (- (:x attacker) (:x victim)) 0) ; victim is left?
        :left (< (- (:x attacker) (:x victim)) 0) ; victim is right?
        false)))

(defn attack
  [entities attacker]
  (let [victim (first (drop-while #(not-victim? attacker %) entities))]
    (map (fn [e]
           (if (= e victim)
             (let [health (max 0 (- (:health e) (:damage attacker)))]
               (assoc e
                      :play-sound (if (and (= health 0) (:death-sound victim))
                                    (:death-sound victim)
                                    (:hurt-sound victim))
                      :health health))
             e))
         entities)))

(defn npc-attacker?
  [entity player]
  (and player
       (:npc? entity)
       (> (:health entity) 0)
       (= (:attack-time entity) 0)
       (near-entity? entity player attack-distance)))

(defn attack-player
  [entities]
  (if-let [npc (some #(if (npc-attacker? % (get-player entities)) %)
                     entities)]
    (attack entities npc)
    entities))

(defn randomize-locations
  [screen entities entity]
  (->> (for [tile-x (range 0 (- map-width (:width entity)))
             tile-y (range 0 (- map-height (:height entity)))]
         {:x tile-x :y tile-y})
       shuffle
       (drop-while #(invalid-location? screen entities (merge entity %)))
       first
       (merge entity {:id (count entities)})
       (conj entities)))

(defn prevent-move
  [entities entity]
  (if (or (= (:health entity) 0)
          (< (:x entity) 0)
          (> (:x entity) (- map-width 1))
          (< (:y entity) 0)
          (> (:y entity) (- map-height 1))
          (and (or (not= 0 (:x-change entity))
                   (not= 0 (:y-change entity)))
               (near-entities? entities entity 1)))
    (assoc entity
           :x-velocity 0
           :y-velocity 0
           :x-change 0
           :y-change 0
           :x (- (:x entity) (:x-change entity))
           :y (- (:y entity) (:y-change entity)))
    entity))

(defn adjust-times
  [screen entity]
  (if-let [attack-time (:attack-time entity)]
    (assoc entity
           :attack-time
           (if (> attack-time 0)
             (max 0 (- attack-time (:delta-time screen)))
             max-attack-time))
    entity))

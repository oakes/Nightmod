(load-game-file "utils.clj")

(defn create
  [grid]
  (let [moves (zipmap directions
                      (map #(animation duration (take 5 %)) grid))
        attacks (zipmap directions (map #(texture (nth % 5)) grid))
        hits (zipmap directions (map #(texture (nth % 6)) grid))
        deads (zipmap directions (map #(texture (nth % 7)) grid))
        start-direction :s]
    (assoc (texture (get-in grid [(.indexOf directions start-direction) 0]))
           :width 2
           :height 2
           :moves moves
           :attacks attacks
           :hits hits
           :deads deads
           :x-velocity 0
           :y-velocity 0
           :x-feet 0
           :y-feet 0
           :last-attack 0
           :attack-interval 1
           :direction start-direction
           :health 10
           :wounds 0
           :damage 2)))

(defn create-player
  []
  (let [path "player.png"
        grid (split-texture path grid-tile-size 192)]
    (assoc (create grid)
           :player? true
           :max-velocity 2
           :attack-interval 0.25
           :health 40
           :x-feet 0.5
           :y-feet 0.5)))

(defn create-enemy
  []
  (let [path "enemy.png"
        grid (split-texture path grid-tile-size 192)]
    (assoc (create grid)
           :npc? true
           :max-velocity 1.5
           :health 40
           :x-feet 0.5
           :y-feet 0.5)))

(defn move
  [screen entities entity]
  (let [[x-velocity y-velocity] (get-velocity entities entity)
        x-change (* x-velocity (:delta-time screen))
        y-change (* y-velocity (:delta-time screen))]
    (cond
      (= (:health entity) 0)
      (assoc entity :x-velocity 0 :y-velocity 0)
      (or (not= 0 x-change) (not= 0 y-change))
      (assoc entity
             :x-velocity (decelerate x-velocity)
             :y-velocity (decelerate y-velocity)
             :x-change x-change
             :y-change y-change
             :x (+ (:x entity) x-change)
             :y (+ (:y entity) y-change))
      :else
      entity)))

(defn recover
  [entity]
  (if (and (>= (:last-attack entity) 0.5) (> (:health entity) 0))
    (merge entity
           (-> (get-in entity [:moves (:direction entity)])
               (animation! :get-key-frame 0)
               texture))
    entity))

(defn animate
  [screen entity]
  (if-let [direction (get-direction (:x-velocity entity)
                                    (:y-velocity entity))]
    (let [anim (get-in entity [:moves direction])]
      (merge entity
             (animation->texture screen anim)
             {:direction direction}))
    (recover entity)))

(defn prevent-move
  [screen entities entity]
  (let [old-x (- (:x entity) (:x-change entity))
        old-y (- (:y entity) (:y-change entity))
        x-entity (assoc entity :y old-y)
        y-entity (assoc entity :x old-x)]
    (merge entity
           (when (invalid-location? screen entities x-entity)
             {:x-velocity 0 :x-change 0 :x old-x})
           (when (invalid-location? screen entities y-entity)
             {:y-velocity 0 :y-change 0 :y old-y}))))

(defn adjust
  [screen entity]
  (let [reset? (and (:npc? entity)
                    (>= (:last-attack entity) (:attack-interval entity)))]
    (assoc entity
           :last-attack (if reset?
                          0
                          (+ (:last-attack entity) (:delta-time screen))))))

(defn attack
  [screen attacker victim entities]
  (map (fn [e]
         (cond
           (= (:id e) (:id attacker))
           (let [direction (or (when victim
                                 (get-direction-to-entity attacker victim))
                               (:direction e))]
             (merge e
                    {:last-attack 0 :direction direction}
                    (when (> (:health e) 0)
                      (get-in e [:attacks direction]))))
           
           (= (:id e) (:id victim))
           (if attacker
             (let [health (max 0 (- (:health victim) (:damage attacker)))]
               (merge e
                      {:last-attack 0
                       :health health
                       :wounds (+ (:wounds victim) (:damage attacker))
                       :play-sound (if (and (= health 0) (:death-sound victim))
                                     (:death-sound victim)
                                     (:hurt-sound victim))}
                      (if (> health 0)
                        (get-in e [:hits (:direction victim)])
                        (get-in e [:deads (:direction victim)]))))
             e)
           
           :else
           e))
         entities))

(defn randomize-location
  [screen entities entity]
  (->> (for [tile-x (range 0 (- map-width (:width entity)))
             tile-y (range 0 (- map-height (:height entity)))]
         (isometric->screen screen {:x tile-x :y tile-y}))
       shuffle
       (drop-while
         #(or (near-entity? (merge entity %) (get-player entities) 5)
              (invalid-location? screen entities (merge entity %))))
       first
       (merge entity)))

(defn randomize-locations
  [screen entities entity]
  (conj entities
        (-> (if (:npc? entity)
              (randomize-location screen entities entity)
              entity)
            (assoc :id (count entities)))))

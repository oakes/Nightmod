(def ^:const vertical-tiles 20)
(def ^:const pixels-per-tile 16)
(def ^:const duration 0.2)
(def ^:const damping 0.5)
(def ^:const max-velocity 6)
(def ^:const max-velocity-npc 3)
(def ^:const deceleration 0.9)
(def ^:const map-width 50)
(def ^:const map-height 50)
(def ^:const background-layer "grass")
(def ^:const max-attack-time 1)
(def ^:const aggro-distance 6)
(def ^:const attack-distance 1.5)

(defn on-layer?
  [screen entity & layer-names]
  (let [layers (map #(tiled-map-layer screen %) layer-names)]
    (->> (for [tile-x (range (int (:x entity))
                             (+ (:x entity) (:width entity)))
               tile-y (range (int (:y entity))
                             (+ (:y entity) (:height entity)))]
           (some #(tiled-map-cell % tile-x tile-y) layers))
         (drop-while nil?)
         (first)
         (nil?)
         (not))))

(defn on-start-layer?
  [screen entity]
  (->> (for [layer-name (map-layer-names screen)]
         (or (= layer-name background-layer)
             (= (on-layer? screen entity layer-name)
                (= layer-name (:start-layer entity)))))
       (drop-while identity)
       (first)
       (nil?)))

(defn near-entity?
  [e e2 min-distance]
  (and (not= (:id e) (:id e2))
       (nil? (:draw-time e2))
       (> (:health e2) 0)
       (< (Math/abs ^double (- (:x e) (:x e2))) min-distance)
       (< (Math/abs ^double (- (:y e) (:y e2))) min-distance)))

(defn near-entities?
  [entities entity min-distance]
  (some #(near-entity? entity % min-distance) entities))

(defn invalid-location?
  [screen entities entity]
  (or (not (on-start-layer? screen entity))
      (near-entities? entities entity (:min-distance entity))))

(defn decelerate
  [velocity]
  (let [velocity (* velocity deceleration)]
    (if (< (Math/abs velocity) damping)
      0
      velocity)))

(defn get-player
  [entities]
  (some #(if (:player? %) %) entities))

(defn touched?
  [key]
  (and (game :touched?)
       (case key
         :down (> (game :y) (* (game :height) (/ 2 3)))
         :up (< (game :y) (/ (game :height) 3))
         :left (< (game :x) (/ (game :width) 3))
         :right (> (game :x) (* (game :width) (/ 2 3)))
         false)))

(defn get-player-velocity
  [entity]
  [(cond
     (or (key-pressed? :dpad-left) (touched? :left)) (* -1 max-velocity)
     (or (key-pressed? :dpad-right) (touched? :right)) max-velocity
     :else (:x-velocity entity))
   (cond
     (or (key-pressed? :dpad-down) (touched? :down)) (* -1 max-velocity)
     (or (key-pressed? :dpad-up) (touched? :up)) max-velocity
     :else (:y-velocity entity))])

(defn get-npc-axis-velocity
  [diff]
  (cond
    (> diff attack-distance) (* -1 max-velocity-npc)
    (< diff (* -1 attack-distance)) max-velocity-npc
    :else 0))

(defn get-npc-aggro-velocity
  [npc player]
  (let [x-diff (- (:x npc) (:x player))
        y-diff (- (:y npc) (:y player))]
    [(get-npc-axis-velocity x-diff)
     (get-npc-axis-velocity y-diff)]))

(defn get-npc-velocity
  [entities entity]
  (let [player (get-player entities)]
    (if (and player (near-entity? entity player aggro-distance))
      (get-npc-aggro-velocity entity player)
      (if (= (:attack-time entity) 0)
        [(* max-velocity-npc (- (rand-int 3) 1))
         (* max-velocity-npc (- (rand-int 3) 1))]
        [(:x-velocity entity) (:y-velocity entity)]))))

(defn get-velocity
  [entities entity]
  (cond
    (:player? entity) (get-player-velocity entity)
    (:npc? entity) (get-npc-velocity entities entity)
    :else [0 0]))

(defn get-direction
  [entity]
  (cond
    (not= (:y-velocity entity) 0) (if (> (:y-velocity entity) 0) :up :down)
    (not= (:x-velocity entity) 0) (if (> (:x-velocity entity) 0) :right :left)
    :else nil))

(defn find-id
  [entities id]
  (some #(if (= id (:id %)) %) entities))

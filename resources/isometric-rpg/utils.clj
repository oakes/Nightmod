(def vertical-tiles 6)
(def pixels-per-tile 64)
(def duration 0.2)
(def damping 0.5)
(def deceleration 0.9)
(def map-width 25)
(def map-height 25)
(def aggro-distance 2)
(def attack-distance 0.25)
(def grid-tile-size 256)

(def directions [:w :nw :n :ne
                 :e :se :s :sw])
(def velocities [[-1 0] [-1 1] [0 1] [1 1]
                 [1 0] [1 -1] [0 -1] [-1 -1]])

(def bar-w 20)
(def bar-h 80)
(def npc-bar-h 0.1)

(defn entity-rect
  [entity min-distance]
  (rectangle (- (+ (:x entity) (:x-feet entity))
                (/ min-distance 4))
             (- (+ (:y entity) (:y-feet entity))
                (/ min-distance 4))
             (- (+ (:width entity) (/ min-distance 2))
                (* 2 (:x-feet entity)))
             (- (+ (:height entity) (/ min-distance 2))
                (* 2 (:y-feet entity)))))

(defn on-object-layer?
  [screen entity layer-name]
  (let [entity (screen->isometric screen entity)
        rect (entity-rect entity 0)
        layer (map-layer screen layer-name)]
    (->> (for [o (map-objects layer)]
           (let [r (map-object! o :get-rectangle)
                 x (/ (rectangle! r :get-x) (/ pixels-per-tile 2))
                 y (/ (rectangle! r :get-y) (/ pixels-per-tile 2))
                 width (/ (rectangle! r :get-width) (/ pixels-per-tile 2))
                 height (/ (rectangle! r :get-height) (/ pixels-per-tile 2))]
             (rectangle! (rectangle x y width height) :overlaps rect)))
         (drop-while not)
         (first)
         (nil?)
         (not))))

(defn near-entity?
  [e e2 min]
  (and (not= (:id e) (:id e2))
       (> (:health e2) 0)
       (rectangle! (entity-rect e min) :overlaps (entity-rect e2 min))))

(defn near-entities?
  [entities entity min-distance]
  (some #(near-entity? entity % min-distance) entities))

(defn invalid-location?
  ([screen entities entity]
    (invalid-location? screen entities entity 0))
  ([screen entities entity min-distance]
    (or (near-entities? entities entity min-distance)
        (on-object-layer? screen entity "barriers"))))

(defn decelerate
  [velocity]
  (let [velocity (* velocity deceleration)]
    (if (< (Math/abs velocity) damping)
      0
      velocity)))

(defn get-player-velocity
  [entity]
  (if (and (game :touched?) (button-pressed? :left))
    (let [x (float (- (game :x) (/ (game :width) 2)))
          y (float (- (/ (game :height) 2) (game :y)))
          x-adjust (* (:max-velocity entity) (Math/abs (double (/ x y))))
          y-adjust (* (:max-velocity entity) (Math/abs (double (/ y x))))]
      [(* (Math/signum x) (min (:max-velocity entity) x-adjust))
       (* (Math/signum y) (min (:max-velocity entity) y-adjust))])
    [(cond
       (key-pressed? :dpad-left) (* -1 (:max-velocity entity))
       (key-pressed? :dpad-right) (:max-velocity entity)
       :else (:x-velocity entity))
     (cond
       (key-pressed? :dpad-down) (* -1 (:max-velocity entity))
       (key-pressed? :dpad-up) (:max-velocity entity)
       :else (:y-velocity entity))]))

(defn get-npc-axis-velocity
  [entity diff]
  (cond
    (> diff attack-distance) (* -1 (:max-velocity entity))
    (< diff (* -1 attack-distance)) (:max-velocity entity)
    :else 0))

(defn get-npc-aggro-velocity
  [npc player]
  (let [r1 (entity-rect npc attack-distance)
        r2 (entity-rect player attack-distance)
        x-diff (- (rectangle! r1 :get-x) (rectangle! r2 :get-x))
        y-diff (- (rectangle! r1 :get-y) (rectangle! r2 :get-y))]
    (if-not (rectangle! r1 :overlaps r2)
      [(get-npc-axis-velocity npc x-diff)
       (get-npc-axis-velocity npc y-diff)]
      [0 0])))

(defn get-npc-velocity
  [entities entity]
  (let [player (find-first :player? entities)]
    (if (near-entity? entity player aggro-distance)
      (get-npc-aggro-velocity entity player)
      (if (>= (:last-attack entity) (:attack-interval entity))
        [(* (:max-velocity entity) (- (rand-int 3) 1))
         (* (:max-velocity entity) (- (rand-int 3) 1))]
        [(:x-velocity entity) (:y-velocity entity)]))))

(defn get-velocity
  [entities entity]
  (cond
    (:player? entity) (get-player-velocity entity)
    (:npc? entity) (get-npc-velocity entities entity)
    :else [0 0]))

(defn get-direction
  [x-velocity y-velocity]
  (some->> velocities
           (filter (fn [[x y]]
                     (and (= x (int (Math/signum (float x-velocity))))
                          (= y (int (Math/signum (float y-velocity)))))))
           (first)
           (.indexOf velocities)
           (nth directions)))

(defn get-direction-to-entity
  [e e2]
  (or (get-direction (- (+ (:x e2) (:x-feet e2))
                        (+ (:x e) (:x-feet e)))
                     (- (+ (:y e2) (:y-feet e2))
                        (+ (:y e) (:y-feet e))))
      (:last-direction e)))

(defn split-texture
  [path size mask-size]
  (let [start (/ (- size mask-size) 2)
        grid (texture! (texture path) :split size size)]
    (doseq [row grid
            item row]
      (texture! item :set-region item start start mask-size mask-size))
    grid))

(defn can-attack?
  [e e2]
  (and e2
       (not= (:npc? e) (:npc? e2))
       (> (:health e) 0)
       (>= (:last-attack e) (:attack-interval e))
       (near-entity? e e2 attack-distance)))

(defn get-entity-at-cursor
  [screen entities input-x input-y]
  (let [cursor-pos (input->screen screen input-x input-y)]
    (some (fn [{:keys [x y width height npc? health] :as entity}]
            (-> (rectangle x y width height)
                (rectangle! :contains (:x cursor-pos) (:y cursor-pos))
                (and npc? (> health 0))
                (when entity)))
          entities)))

(defn sort-entities
  [entities]
  (sort-by :y #(compare %2 %1) entities))

(def vertical-tiles 20)
(def duration 0.15)
(def damping 0.5)
(def max-velocity 14)
(def max-jump-velocity 56)
(def deceleration 0.9)
(def gravity -2.5)

(defn decelerate
  [velocity]
  (let [velocity (* velocity deceleration)]
    (if (< (Math/abs velocity) damping)
      0
      velocity)))

(defn touched?
  [key]
  (and (game :touched?)
       (case key
         :down (> (game :y) (* (game :height) (/ 2 3)))
         :up (< (game :y) (/ (game :height) 3))
         :left (< (game :x) (/ (game :width) 3))
         :right (> (game :x) (* (game :width) (/ 2 3)))
         false)))

(defn get-x-velocity
  [entity]
  (if (:player? entity)
    (cond
      (or (key-pressed? :dpad-left) (touched? :left))
      (* -1 max-velocity)
      (or (key-pressed? :dpad-right) (touched? :right))
      max-velocity
      :else
      (:x-velocity entity))
    (:x-velocity entity)))

(defn get-y-velocity
  [entity]
  (if (:player? entity)
    (cond
      (and (:can-jump? entity)
           (or (key-pressed? :dpad-up) (touched? :up)))
      max-jump-velocity
      :else
      (:y-velocity entity))
    (:y-velocity entity)))

(defn get-direction
  [entity]
  (cond
    (> (:x-velocity entity) 0) :right
    (< (:x-velocity entity) 0) :left
    :else (:direction entity)))

(defn get-touching-tile
  [screen entity & layer-names]
  (let [layers (map #(tiled-map-layer screen %) layer-names)]
    (->> (for [tile-x (range (int (:x entity))
                             (+ (:x entity) (:width entity)))
               tile-y (range (int (:y entity))
                             (+ (:y entity) (:height entity)))]
           (some #(when (tiled-map-cell % tile-x tile-y)
                    [tile-x tile-y])
                 layers))
         (drop-while nil?)
         first)))

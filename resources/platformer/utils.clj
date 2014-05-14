(def ^:const vertical-tiles 20)
(def ^:const duration 0.15)
(def ^:const damping 0.5)
(def ^:const max-velocity 14)
(def ^:const max-jump-velocity (* max-velocity 4))
(def ^:const deceleration 0.9)
(def ^:const gravity -2.5)

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
  [{:keys [me? x-velocity]}]
  (if me?
    (cond
      (or (key-pressed? :dpad-left) (touched? :left))
      (* -1 max-velocity)
      (or (key-pressed? :dpad-right) (touched? :right))
      max-velocity
      :else
      x-velocity)
    x-velocity))

(defn get-y-velocity
  [{:keys [me? y-velocity can-jump?]}]
  (if me?
    (cond
      (and can-jump? (or (key-pressed? :dpad-up) (touched? :up)))
      max-jump-velocity
      :else
      y-velocity)
    y-velocity))

(defn get-direction
  [{:keys [x-velocity direction]}]
  (cond
    (> x-velocity 0) :right
    (< x-velocity 0) :left
    :else
    direction))

(defn get-touching-tile
  [screen {:keys [x y width height]} & layer-names]
  (let [layers (map #(tiled-map-layer screen %) layer-names)]
    (->> (for [tile-x (range (int x) (+ x width))
               tile-y (range (int y) (+ y height))]
           (some #(when (tiled-map-cell % tile-x tile-y)
                    [tile-x tile-y])
                 layers))
         (drop-while nil?)
         first)))

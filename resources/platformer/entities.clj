(load-game-file "utils.clj")

(defn create-player
  []
  (let [stand (texture "player_stand.png")
        jump (texture "player_jump.png")
        walk [(texture "player_walk1.png")
              (texture "player_walk2.png")
              (texture "player_walk3.png")]]
    (assoc stand
           :stand-right stand
           :stand-left (texture stand :flip true false)
           :jump-right jump
           :jump-left (texture jump :flip true false)
           :walk-right (animation duration
                                  walk
                                  :set-play-mode (play-mode :loop-pingpong))
           :walk-left (animation duration
                                 (map #(texture % :flip true false) walk)
                                 :set-play-mode (play-mode :loop-pingpong))
           :width 1
           :height (/ 26 18)
           :x-velocity 0
           :y-velocity 0
           :x 12
           :y 10
           :me? true
           :can-jump? false
           :direction :right)))

(defn move
  [{:keys [delta-time]} {:keys [x y can-jump?] :as entity}]
  (let [x-velocity (get-x-velocity entity)
        y-velocity (+ (get-y-velocity entity) gravity)
        x-change (* x-velocity delta-time)
        y-change (* y-velocity delta-time)]
    (if (or (not= 0 x-change) (not= 0 y-change))
      (assoc entity
             :x-velocity (decelerate x-velocity)
             :y-velocity (decelerate y-velocity)
             :x-change x-change
             :y-change y-change
             :x (+ x x-change)
             :y (+ y y-change)
             :can-jump? (if (> y-velocity 0) false can-jump?))
      entity)))

(defn animate
  [screen {:keys [x-velocity y-velocity
                  stand-right stand-left
                  jump-right jump-left
                  walk-right walk-left] :as entity}]
  (let [direction (get-direction entity)]
    (merge entity
           (cond
             (not= y-velocity 0)
             (if (= direction :right) jump-right jump-left)
             (not= x-velocity 0)
             (if (= direction :right)
               (animation->texture screen walk-right)
               (animation->texture screen walk-left))
             :else
             (if (= direction :right) stand-right stand-left))
           {:direction direction})))

(defn prevent-move
  [screen {:keys [x y x-change y-change] :as entity}]
  (let [old-x (- x x-change)
        old-y (- y y-change)
        entity-x (assoc entity :y old-y)
        entity-y (assoc entity :x old-x)
        up? (> y-change 0)]
    (merge entity
           (when (get-touching-tile screen entity-x "walls")
             {:x-velocity 0 :x-change 0 :x old-x})
           (when (get-touching-tile screen entity-y "walls")
             {:y-velocity 0 :y-change 0 :y old-y :can-jump? (not up?)}))))

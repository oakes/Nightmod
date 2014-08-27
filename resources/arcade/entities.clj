(defn touching?
  "Returns true if the entities are touching each other."
  [e1 e2]
  (rectangle! (rectangle (:x e1) (:y e1) (:width e1) (:height e1))
              :overlaps
              (rectangle (:x e2) (:y e2) (:width e2) (:height e2))))

(defn touching-entities
  "Returns the entities that are touching the given entity."
  [entity entities]
  (filter #(touching? entity %) entities))

(defn remove-enemy?
  "Returns true if the player or a missle is touching the given entity."
  [entity entities]
  (or (some :player? (touching-entities entity entities))
      (some :missile? (touching-entities entity entities))))

(defn remove-missile?
  "Returns true if an enemy is touching the given entity."
  [entity entities]
  (some :enemy? (touching-entities entity entities)))

(defn create-missile
  "Returns a missile at the same x position as the mouse."
  []
  (assoc (shape :filled
                :set-color (color :blue)
                :circle 0 0 10)
         :missile? true
         :x (game :point-x)
         :y 50
         :width 10
         :height 10))

(defn move-missiles
  "Moves the missiles up."
  [entities]
  (for [e entities]
    (if (:missile? e)
      (assoc e :y (+ (:y e) 5))
      e)))

(defn remove-missiles
  "Marks missiles for removal if they are too high or are touching an enemy."
  [entities]
  (for [e entities]
    (if (and (:missile? e)
             (or (> (:y e) (game :height))
                 (remove-missile? e entities)))
      (assoc e :remove? true)
      e)))

(defn create-enemy
  "Returns an enemy at a random position."
  []
  (assoc (texture "enemy.png")
         :enemy? true
         :x (rand (game :width))
         :y (game :height)
         :width 64
         :height 64))

(defn move-enemies
  "Moves the enemies down."
  [entities]
  (for [e entities]
    (if (:enemy? e)
      (assoc e :y (- (:y e) 3))
      e)))

(defn remove-enemies
  "Marks enemies for removal if they are too low or are touching a missile or
player."
  [entities]
  (for [e entities]
    (if (and (:enemy? e)
             (or (< (:y e) 0)
                 (remove-enemy? e entities)))
      (assoc e :remove? true)
      e)))

(defn touching?
  [e1 e2]
  ; figure out whether the given entities overlap each other
  (rectangle! (rectangle (:x e1) (:y e1) (:width e1) (:height e1))
              :overlaps
              (rectangle (:x e2) (:y e2) (:width e2) (:height e2))))

(defn touching-entities
  [entity entities]
  ; return the entities that the entity is touching, or nil if none is found
  (filter (fn [e] (touching? entity e)) entities))

(defn remove-enemy?
  [entity entities]
  ; return the first player or missile touching this entity
  (or (some :player? (touching-entities entity entities))
      (some :missile? (touching-entities entity entities))))

(defn remove-missile?
  [entity entities]
  ; return the first enemy touching this entity
  (some :enemy? (touching-entities entity entities)))

(defn create-missile
  []
  ; create a circle shape and set its x position to where the mouse is
  (assoc (shape :filled
                :set-color (color :blue)
                :circle 0 0 10)
         :missile? true
         :x (game :x)
         :y 50
         :width 10
         :height 10))

(defn move-missiles
  [entities]
  ; move the missiles up
  (for [e entities]
    (if (:missile? e)
      (assoc e :y (+ (:y e) 5))
      e)))

(defn remove-missiles
  [entities]
  ; mark missiles for removal if they are too high or are touching an enemy
  (for [e entities]
    (if (and (:missile? e)
             (or (> (:y e) (game :height))
                 (remove-missile? e entities)))
      (assoc e :remove? true)
      e)))

(defn create-enemy
  []
  (assoc (texture (rand-nth ["enemy.png"]))
         :enemy? true
         :x (rand (game :width))
         :y (game :height)
         :width 64
         :height 64))

(defn move-enemies
  [entities]
  ; move the enemies down
  (for [e entities]
    (if (:enemy? e)
      (assoc e :y (- (:y e) 3))
      e)))

(defn remove-enemies
  [entities]
  ; mark enemies for removal if they are too low
  ; or are touching a missile/player
  (for [e entities]
    (if (and (:enemy? e)
             (or (< (:y e) 0)
                 (remove-enemy? e entities)))
      (assoc e :remove? true)
      e)))

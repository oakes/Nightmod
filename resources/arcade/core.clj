(load-game-file "entities.clj")

(def score (atom 0))

(defn update-score!
  [entities]
  (doseq [e entities]
    (when (and (:enemy? e) (remove-enemy? e entities))
      (swap! score inc))
    (when (and (:enemy? e) (< (:y e) 0))
      (swap! score dec)))
  entities)

(defn update-label!
  [entities]
  (doseq [e entities]
    (when (:score? e)
      (label! e :set-text (str @score)))))

(defscreen main-screen
  ; runs when the screen first shows
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    ; set a timer to spawn an enemy every 2 seconds
    (add-timer! screen :spawn-enemy 0 2)
    (assoc (texture "player.png")
           :player? true
           :x (/ (game :width) 2)
           :y 10
           :width 64
           :height 64))
  ; runs every frame (many times per second)
  :on-render
  (fn [screen entities]
    (clear!)
    (->> entities
         (move-missiles)
         (move-enemies)
         (update-score!)
         (remove-missiles)
         (remove-enemies)
         (remove :remove?)
         (render! screen)))
  ; runs when the mouse is moved
  :on-mouse-moved
  (fn [screen entities]
    ; set the player's x position to the mouse position,
    ; minus half of the player's width
    (for [e entities]
      (if (:player? e)
        (assoc e :x (- (:input-x screen)
                       (/ (texture! e :get-region-width) 2)))
        e)))
  ; runs when a key is pressed
  :on-key-down
  (fn [screen entities]
    (when (= (:key screen) (key-code :space))
      (conj entities (create-missile))))
  ; runs when the mouse is clicked
  :on-touch-down
  (fn [screen entities]
    (conj entities (create-missile)))
  ; runs when the timer executes
  :on-timer
  (fn [screen entities]
    (conj entities (create-enemy))))

(defscreen score-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (assoc (label "0" (color :white))
           :score? true
           :x 5))
  :on-render
  (fn [screen entities]
    (update-label! entities)
    (render! screen entities))
  :on-resize
  (fn [screen entities]
    (height! screen 300)))

(set-game-screen! main-screen score-screen)

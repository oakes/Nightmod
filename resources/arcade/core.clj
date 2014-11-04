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
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage) :camera (orthographic))
    (add-timer! screen :spawn-enemy 0 2)
    (assoc (texture "player.png")
           :player? true
           :x (/ (game :width) 2)
           :y 10
           :width 64
           :height 64))
  
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
  
  :on-resize
  (fn [screen entities]
    (height! screen (:height screen)))
  
  :on-mouse-moved
  (fn [screen entities]
    (for [e entities]
      (if (:player? e)
        (assoc e :x (- (game :x) (/ (texture! e :get-region-width) 2)))
        e)))
  
  :on-key-down
  (fn [screen entities]
    (when (= (:key screen) (key-code :space))
      (conj entities (create-missile))))
  
  :on-touch-down
  (fn [screen entities]
    (conj entities (create-missile)))
  
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

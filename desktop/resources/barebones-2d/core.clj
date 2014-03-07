(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (conj entities (label "Hello world!" (color :white))))
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities)))

(set-game-screen! main-screen)

(ns nightmod.game
  (:require [clojail.core :as jail]
            [clojure.java.io :as io]
            [nightmod.manager :as manager]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]))

(defn set-game-screen!
  "Displays one or more screens that you defined with `defscreen`. They will be
run the order they are passed in. Note that only the first screen should call
`(clear!)` in its :on-render function; if the others do, they will clear
whatever was drawn by the preceding screens.

    (set-game-screen! main-screen text-screen)"
  [& game-screens]
  (manager/stop-timers!)
  (->> (conj (vec game-screens) screens/overlay-screen)
       (apply set-screen! screens/nightmod)
       on-gl))

(defn restart-game!
  "Causes the core.clj file to be run again."
  []
  (on-gl (screens/restart!)))

(defmacro load-game-file
  "Loads a file into the namespace.

    (load-game-file \"utils.clj\")"
  [n]
  (some->> (io/file @u/project-dir n)
           slurp
           (format "(do %s\n)")
           jail/safe-read))

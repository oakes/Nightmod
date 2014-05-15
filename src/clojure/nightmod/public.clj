(ns nightmod.public
  (:require [clojail.core :as jail]
            [clojure.java.io :as io]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]))

(defn set-game-screen!
  [& game-screens]
  (->> (conj (vec game-screens) screens/overlay-screen)
       (apply set-screen! screens/nightmod)
       on-gl))

(defn restart-game!
  []
  (on-gl (screens/restart!)))

(defmacro load-game-file
  [n]
  (some->> (io/file @u/project-dir n)
           slurp
           (format "(do %s\n)")
           jail/safe-read))

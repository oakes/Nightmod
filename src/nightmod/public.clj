(ns nightmod.public
  (:require [nightmod.core :refer :all]
            [play-clj.core :refer :all]))

(defn set-game-screen!
  [& screens]
  (->> (apply set-screen! nightmod (conj (vec screens) overlay-screen))
       (fn [])
       (app! :post-runnable)))

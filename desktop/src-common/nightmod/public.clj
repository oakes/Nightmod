(ns nightmod.public
  (:require [nightmod.core :refer :all]
            [play-clj.core :refer :all]))

(defgame nightmod
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
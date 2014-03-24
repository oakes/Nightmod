(ns nightmod.public
  (:require [clojail.core :as jail]
            [clojure.java.io :as io]
            [nightmod.screens :as s]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]))

(defn set-game-screen!
  [& screens]
  (->> (conj (vec screens) s/overlay-screen)
       (apply set-screen! s/nightmod)
       (fn [])
       (app! :post-runnable)))

(defmacro load-game-file
  [n]
  (some->> (or (io/resource n)
               (throw (Throwable. (str "File not found: " n))))
           slurp
           (format "(do %s\n)")
           jail/safe-read))

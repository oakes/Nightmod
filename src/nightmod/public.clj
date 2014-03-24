(ns nightmod.public
  (:require [clojail.core :as jail]
            [clojail.jvm :as jvm]
            [clojure.java.io :as io]
            [nightmod.sandbox :as sandbox]
            [nightmod.screens :as s]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]))

(intern 'play-clj.core
        'wrapper
        (fn [screen f]
          (if (= screen (:screen s/overlay-screen))
            (f)
            (try
              (jvm/jvm-sandbox f sandbox/context)
              (catch Exception e
                (when (nil? @u/error) (reset! u/error e)))))))

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

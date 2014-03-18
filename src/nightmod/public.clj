(ns nightmod.public
  (:require [clojail.core :as jail]
            [clojail.jvm :as jvm]
            [clojure.java.io :as io]
            [nightmod.sandbox :as sandbox]
            [nightmod.screens :as s]
            [nightmod.utils :as u]
            [play-clj.core :refer :all])
  (:import [java.security AccessController]))

(defn set-game-screen!
  [& screens]
  (->> (fn [f args]
         (try
           (jvm/jvm-sandbox #(apply f args) sandbox/context)
           (catch Exception e
             (when (nil? @u/error) (reset! u/error e)))))
       (set-screen-with-options! s/nightmod
                                 (conj (vec screens) s/overlay-screen)
                                 :wrap)
       (fn [])
       (app! :post-runnable)))

(defmacro load-game-file
  [n]
  (some->> (or (io/resource n)
               (throw (Throwable. (str "File not found: " n))))
           slurp
           (format "(do %s\n)")
           jail/safe-read))

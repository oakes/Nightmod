(ns nightmod.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all])
  (:import [java.text SimpleDateFormat]))

(def ^:const templates ["arcade" "platformer"
                        "orthogonal-rpg" "isometric-rpg"
                        "barebones-2d" "barebones-3d"])

(defn get-data-dir
  []
  (let [home-dir (System/getProperty "user.home")
        app-name "Nightmod"
        app-name-lower (clojure.string/lower-case app-name)
        osx-dir (io/file home-dir "Library" "Application Support" app-name)
        win-dir (io/file home-dir "AppData" "Roaming" app-name)]
    (.getCanonicalPath
      (cond
        (.exists (.getParentFile osx-dir)) osx-dir
        (.exists (.getParentFile win-dir)) win-dir
        :else (if-let [config-dir (System/getenv "XDG_CONFIG_HOME")]
                (io/file config-dir app-name-lower)
                (io/file home-dir ".config" app-name-lower))))))

(defn format-date
  [unix-time]
  (.format (SimpleDateFormat. "yyyy.MM.dd HH:mm:ss") unix-time))

(defn read-title
  [f]
  [(.getCanonicalPath f)
   (or (-> (io/file f ".properties")
            slurp
            edn/read-string
            :title
            (try (catch Exception _)))
       (-> (.getName f)
           Long/parseLong
           format-date
           (try (catch Exception _)))
       "Invalid")])

(defn new-project!
  [template]
  (println "New Project:" template))

(defn load-project!
  [path]
  (println "Load Project:" path))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage) :camera (orthographic))
    (let [ui-skin (skin "uiskin.json")
          projects-dir (doto (io/file (get-data-dir) "projects") .mkdirs)
          create-button (fn [[k v]]
                          (text-button v ui-skin :set-name k))
          template-names ["Arcade" "Platformer"
                          "Orthogonal RPG" "Isometric RPG"
                          "Barebones 2D" "Barebones 3D"]
          new-games (->> (for [i (range (count templates))]
                           [(nth templates i)
                            (nth template-names i)])
                         (map create-button))
          saved-games (->> (.listFiles projects-dir)
                           (filter #(.isDirectory %))
                           (map read-title)
                           (map create-button))]
      (-> (cons (label "New Game:" ui-skin) new-games)
          (concat (when (seq saved-games)
                    (cons (label "Load Game:" ui-skin) saved-games)))
          vertical
          (scroll-pane (style :scroll-pane nil nil nil nil nil))
          list
          (table :align (align :center) :set-fill-parent true))))
  :on-render
  (fn [screen entities]
    (clear! 0 0 0 0)
    (render! screen entities))
  :on-resize
  (fn [screen entities]
    (height! screen 400))
  :on-ui-changed
  (fn [screen entities]
    (when-let [n (-> screen :actor .getName)]
      (if (contains? (set templates) n)
        (new-project! n)
        (load-project! n)))))

(defgame nightmod
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

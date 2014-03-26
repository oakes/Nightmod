(ns nightmod.screens
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]))

(declare nightmod main-screen blank-screen overlay-screen)

(def ^:const templates ["arcade" "platformer"
                        "orthogonal-rpg" "isometric-rpg"
                        "barebones-2d" "barebones-3d"])

(defn read-title
  [f]
  [(.getCanonicalPath f)
   (or (-> (io/file f u/properties-file)
            slurp
            edn/read-string
            :title
            (try (catch Exception _)))
       (-> (.getName f)
           Long/parseLong
           u/format-date
           (try (catch Exception _)))
       "Invalid")])

(defn load-project!
  [path]
  (reset! u/project-dir path))

(defn new-project!
  [template]
  (load-project! (u/new-project! template)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage) :camera (orthographic))
    (when-not @u/main-dir
      (reset! u/main-dir (u/get-data-dir)))
    (let [ui-skin (skin "uiskin.json")
          create-button (fn [[k v]]
                          (text-button v ui-skin :set-name k))
          template-names ["Arcade" "Platformer"
                          "Orthogonal RPG" "Isometric RPG"
                          "Barebones 2D" "Barebones 3D"]
          new-games (->> (for [i (range (count templates))]
                           [(nth templates i)
                            (nth template-names i)])
                         (map create-button))
          saved-games (->> (io/file @u/main-dir)
                           .listFiles
                           (filter #(.isDirectory %))
                           (map read-title)
                           (map create-button))]
      (-> (cons (label "New Game:" ui-skin) new-games)
          (concat (when (seq saved-games)
                    (cons (label "Load Game:" ui-skin) saved-games)))
          (vertical :pack)
          (scroll-pane (style :scroll-pane nil nil nil nil nil))
          vector
          (table :align (align :center) :set-fill-parent true))))
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities))
  :on-resize
  (fn [screen entities]
    (height! screen (:height screen)))
  :on-ui-changed
  (fn [screen entities]
    (when-let [n (text-button! (:actor screen) :get-name)]
      (if (contains? (set templates) n)
        (new-project! n)
        (load-project! n)))
    nil))

(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(defscreen overlay-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (let [ui-skin (skin "uiskin.json")]
      [(-> [(text-button "Home" ui-skin :set-name "home")
            (text-button "Files" ui-skin :set-name "files")]
           (vertical :pack)
           (assoc :id :menu :x 5))
       (-> (label "" ui-skin :set-wrap true)
           (assoc :id :error :x 5 :y 5))]))
  :on-render
  (fn [screen entities]
    (->> (for [e entities]
           (case (:id e)
             :error (doto e
                      (label! :set-text (or (some-> @u/error .toString) ""))
                      (label! :pack))
             e))
         (render! screen)))
  :on-resize
  (fn [screen entities]
    (height! screen (:height screen))
    (for [e entities]
      (case (:id e)
        :menu (assoc e :y (- (:height screen) (vertical! e :get-height)))
        :error (assoc e :width (:width screen))
        e)))
  :on-ui-changed
  (fn [screen entities]
    (case (text-button! (:actor screen) :get-name)
      "home" (do (u/toggle-glass! false)
               (set-screen! nightmod main-screen))
      "files" (u/toggle-glass!)
      nil)
    nil))

(defgame nightmod
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

(add-watch u/error
           :show-error
           (fn [_ _ _ e]
             (when e
               (->> (set-screen! nightmod blank-screen overlay-screen)
                    (fn [])
                    (app! :post-runnable)))))

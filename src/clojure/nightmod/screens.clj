(ns nightmod.screens
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [nightcode.editors :as editors]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [seesaw.core :as s]))

(declare nightmod main-screen blank-screen overlay-screen)

(def ^:const border-space 5)

(defn read-title
  [f]
  [(or (-> (io/file f u/settings-file)
            slurp
            edn/read-string
            :title
            (try (catch Exception _)))
       (-> (.getName f)
           Long/parseLong
           u/format-date
           (try (catch Exception _)))
       (.getName f))
   (.getCanonicalPath f)])

(defn load-project!
  [path]
  (on-gl (set-screen! nightmod blank-screen overlay-screen))
  ; save in project-dir so the reset button works
  (reset! u/project-dir path)
  ; save in tree-projects so the up button is hidden correctly
  (swap! ui/tree-projects conj path)
  ; save in tree-selection so the file grid is displayed
  (s/invoke-now
    (reset! ui/tree-selection path)))

(defn new-project!
  [template]
  (load-project! (u/new-project! template)))

(defn home!
  []
  (s/invoke-now
    (editors/remove-editors! @u/project-dir))
  (u/toggle-glass! false)
  (set-screen! nightmod main-screen))

(defn restart!
  []
  (reset! u/project-dir @u/project-dir))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage) :camera (orthographic))
    (when-not @u/main-dir
      (reset! u/main-dir (u/get-data-dir)))
    (let [ui-skin (skin "uiskin.json")
          create-button (fn [[display-name path]]
                          (text-button display-name ui-skin :set-name path))
          saved-games (->> (io/file @u/main-dir)
                           .listFiles
                           (filter #(.isDirectory %))
                           (sort-by #(.getName %))
                           (map read-title)
                           (map create-button))
          new-games (->> (for [i (range (count u/templates))
                               :let [template (nth u/templates i)]]
                           [(nc-utils/get-string template) template])
                         (map create-button))]
      (-> (when (seq saved-games)
            (cons (label "Load Game:" ui-skin) saved-games))
          (concat (cons (label "New Game:" ui-skin) new-games))
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
      (if (contains? (set u/templates) n)
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
      [(-> (label "" ui-skin
                  :set-wrap true
                  :set-alignment (bit-or (align :left) (align :bottom)))
           (scroll-pane (style :scroll-pane nil nil nil nil nil))
           (assoc :id :error :x border-space :y border-space))
       (-> [(text-button "Home" ui-skin :set-name "home")
            (text-button "Restart" ui-skin :set-name "restart")
            (text-button "Files" ui-skin :set-name "files")]
           (vertical :pack)
           (assoc :id :menu :x border-space))]))
  :on-render
  (fn [screen entities]
    (->> (for [e entities]
           (case (:id e)
             :error (let [l (-> e (scroll-pane! :get-children) first)]
                      (label! l :set-text (or (some-> @u/error .toString) ""))
                      (assoc e :width (if (.isVisible (u/glass))
                                        (- (game :width) u/editor-width)
                                        (game :width))))
             e))
         (render! screen)))
  :on-resize
  (fn [{:keys [width height] :as screen} entities]
    (height! screen height)
    (for [e entities]
      (case (:id e)
        :menu (assoc e :y (- height
                             (vertical! e :get-height)
                             border-space))
        :error (assoc e :width width :height height)
        e)))
  :on-ui-changed
  (fn [screen entities]
    (case (text-button! (:actor screen) :get-name)
      "home" (home!)
      "restart" (restart!)
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
               (on-gl (set-screen! nightmod blank-screen overlay-screen)))))

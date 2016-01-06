(ns nightmod.screens
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [nightcode.dialogs :as dialogs]
            [nightcode.editors :as editors]
            [nightcode.file-browser :as file-browser]
            [nightcode.git :as git]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [nightmod.manager :as manager]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.g3d-physics :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.utils]
            [seesaw.core :as s])
  (:import [com.badlogic.gdx.assets.loaders FileHandleResolver]
           [com.badlogic.gdx.graphics Cursor$SystemCursor Texture]
           [java.awt Toolkit]
           [java.awt.datatransfer Clipboard ClipboardOwner DataFlavor
            StringSelection]
           [java.io File]))

(declare nightmod main-screen blank-screen overlay-screen)

(def ^:const button-height 40)
(def ^:const pad-small 5.0)
(def ^:const pad-large 20.0)
(def ^:const tile-size 250)
(def ^:const col-count 3)

(def templates ["platformer"
                "orthogonal"
                "isometric"
                "arcade"
                "barebones"
                "download"])

(def manager (asset-manager*
               (reify FileHandleResolver
                 (resolve [this file-name]
                   (if (io/resource file-name)
                     (files! :internal file-name)
                     (files! :absolute file-name))))))

; helpers

(defn new-cursor
  [path]
  (try (graphics! :new-cursor (pixmap path) 0 0)
    (catch Exception _)))

(defn set-cursor!
  [cursor]
  (try
    (if cursor
      (graphics! :set-cursor cursor)
      (graphics! :set-system-cursor Cursor$SystemCursor/Arrow))
    (catch Exception _)))

(defn take-screenshot!
  [screen]
  (update! screen :screenshot? false)
  (->> (io/file @u/project-dir u/screenshot-file)
       .getCanonicalPath
       (files! :absolute)
       screenshot!)
  (sound! (:screenshot-sound screen) :play)
  (s/invoke-later
    (file-browser/update-card!)))

(defn out-str
  []
  (->> [(when (seq @u/out) @u/out)
        (some-> @u/error :message)
        (some-> @u/error :exception .toString)
        (when (and @u/error @u/stack-trace?)
          (for [elem (-> @u/error :exception .getStackTrace)]
            (.toString elem)))]
       flatten
       (remove nil?)
       (string/join \newline)))

(defn texture-drawable
  [s]
  (drawable :texture-region (:object (texture s))))

(defn get-center-texture
  [t]
  (let [w (texture! t :get-region-width)
        h (texture! t :get-region-height)
        x (if (> w h) (-> (- w h) (/ 2)) 0)
        y (if (> h w) (-> (- h w) (/ 2)) 0)
        size (if (> w h) h w)]
    (texture t :set-region (int x) (int y) size size)))

(defn read-load-tile
  [f]
  {:display-name (or (-> (io/file f u/settings-file)
                         slurp
                         edn/read-string
                         :title
                         (try (catch Exception _)))
                     (.getName f))
   :name (.getCanonicalPath f)
   :image (let [f (io/file f u/screenshot-file)
                path (.getCanonicalPath f)]
            (some->> (or (try (play-clj.utils/load-asset path Texture)
                           (catch Exception _))
                         (try (Texture. (files! :absolute path))
                           (catch Exception _)))
                     texture
                     get-center-texture
                     :object
                     (drawable :texture-region)))})

(defn read-new-tile
  [name]
  {:display-name (nc-utils/get-string name)
   :name name
   :image (try (texture-drawable (str name "/" u/screenshot-file))
            (catch Exception _))})

(defn wrap-label!
  [btn]
  (doto (image-text-button! btn :get-label-cell)
    (cell! :size tile-size tile-size))
  (doto (image-text-button! btn :get-label)
    (label! :set-wrap true)))

(defn create-tile
  [{:keys [font display-name name image]}]
  [(doto (image-text-button display-name
                            (style :image-text-button image nil nil font)
                            :set-name name)
     wrap-label!)
   :width tile-size
   :height tile-size
   :pad pad-small pad-small pad-small pad-small])

(defn set-visible!
  [entity show?]
  (doseq [a (actor! entity :get-children)]
    (actor! a :set-visible show?)))

(def ^:dynamic *set-blank-screen?* true) ; prevents ∞ loop from :on-hide errors

(defn set-blank-screen!
  []
  (when *set-blank-screen?*
    (binding [*set-blank-screen?* false]
      (set-screen! nightmod blank-screen overlay-screen))))

; tiles on the main screen

(defn load-project!
  [path]
  ; clear the custom cursor and start with a blank screen
  (on-gl (set-cursor! nil)
         (set-blank-screen!))
  ; save in project-dir so the asset loading and reset button works
  (reset! u/project-dir path)
  ; save in tree-projects so the up button is hidden correctly
  (swap! ui/tree-projects conj path)
  ; save in tree-selection so the file grid is displayed
  (s/invoke-later (reset! ui/tree-selection path)))

(defn new-project!
  [template]
  (s/invoke-later
    (when-let [project-name (u/new-project-name! template)]
      (some->> (u/new-project-dir! project-name)
               (u/new-project! template project-name)
               load-project!))))

(defn clone-project!
  [uri-str]
  (s/invoke-later
    (if-let [name (and uri-str (git/address->name uri-str))]
      (when-let [project-name (u/new-project-name! name)]
        (some->> (u/new-project-dir! project-name)
                 (git/clone-with-dialog! uri-str)
                 load-project!))
      (u/show-simple-dialog! (nc-utils/get-string :invalid-git-address)))))

; buttons on the overlay screen

(defn home!
  []
  (when @ui/tree-selection
    (s/invoke-later
      (let [unsaved-paths (editors/unsaved-paths)]
        (when (or (= 0 (count unsaved-paths))
                  (dialogs/show-close-file-dialog! unsaved-paths))
          (editors/remove-editors! @u/project-dir)
          (reset! ui/tree-selection nil)
          (u/toggle-editor! false)
          (on-gl
            (asset-manager! manager :clear)
            (set-screen! nightmod main-screen)
            (set-cursor! nil)
            (manager/clean!)))))))

(defn restart!
  []
  (when @ui/tree-selection
    (reset! u/project-dir @u/project-dir)))

(defn schedule-screenshot!
  []
  (when @ui/tree-selection
    (-> overlay-screen :screen (swap! assoc :screenshot? true))
    nil))

(defn toggle-files!
  []
  (when @ui/tree-selection
    (let [selected? (.exists (io/file @ui/tree-selection))]
      (s/invoke-later
        (when (or selected? (not (.isVisible @u/editor)))
          (u/toggle-editor!))
        (if selected?
          (reset! ui/tree-selection @ui/tree-selection)
          (reset! ui/tree-selection @u/project-dir))
        (some-> (or (editors/get-selected-text-area)
                    @u/editor)
                s/request-focus!)))))

(defn toggle-docs!
  []
  (when @ui/tree-selection
    (let [selected? (= @ui/tree-selection u/docs-name)]
      (s/invoke-later
        (when (or selected? (not (.isVisible @u/editor)))
          (u/toggle-editor!))
        (when-not selected?
          (reset! ui/tree-selection u/docs-name))
        (some-> (s/select @ui/root [:#editor-pane])
                (s/show-card! u/docs-name))
        (some-> (s/select @ui/root [:#docs-sidebar])
                s/request-focus!)))))

(defn toggle-repl!
  []
  (when @ui/tree-selection
    (let [selected? (= @ui/tree-selection u/repl-name)]
      (s/invoke-later
        (when (or selected? (not (.isVisible @u/editor)))
          (u/toggle-editor!))
        (when-not selected?
          (reset! ui/tree-selection u/repl-name))
        (some-> (s/select @ui/root [:#editor-pane])
                (s/show-card! u/repl-name))
        (some-> (s/select @ui/root [:#repl-console])
                s/request-focus!)))))

(defn get-clipboard
  []
  (-> (Toolkit/getDefaultToolkit)
      .getSystemClipboard
      (.getData DataFlavor/stringFlavor)
      (try (catch Exception _))))

(defn set-clipboard!
  [s]
  (let [clip-board (.getSystemClipboard (Toolkit/getDefaultToolkit))]
    (.setContents clip-board
      (StringSelection. s)
      (reify ClipboardOwner
        (lostOwnership [this clipboard contents])))))

; screens

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (binding [play-clj.utils/*asset-manager* manager]
      (update! screen
               :renderer (stage)
               :camera (orthographic)
               :cursor (new-cursor "glove.png")
               :click-sound (sound "click.ogg"))
      (let [ui-skin (skin "uiskin.json")
            default-font (skin! ui-skin :get-font "default-font")
            large-font (skin! ui-skin :get-font "large-font")
            saved-games (->> (io/file @u/main-dir)
                             .listFiles
                             (filter #(.isDirectory %))
                             (sort-by #(.getName %))
                             (map read-load-tile)
                             (map #(assoc % :font default-font))
                             (map create-tile)
                             (partition-all col-count)
                             (map #(cons :row %))
                             (apply concat))
            new-games (->> templates
                           (map read-new-tile)
                           (map #(assoc % :font default-font))
                           (map create-tile)
                           (partition-all col-count)
                           (map #(cons :row %))
                           (apply concat))]
        [(assoc (shape :filled) :id :background)
         (-> (concat (when (seq saved-games)
                       [[(label (nc-utils/get-string :load)
                                (style :label large-font (color :white)))
                         :colspan col-count
                         :pad-top pad-large
                         :pad-bottom pad-large]])
                     saved-games
                     [:row
                      [(label (nc-utils/get-string :new)
                              (style :label large-font (color :white)))
                       :colspan col-count
                       :pad-top pad-large
                       :pad-bottom pad-large]]
                     new-games)
             (table :align (align :center) :pad pad-small)
             (scroll-pane ui-skin
                          :set-fade-scroll-bars false
                          :set-fill-parent true))])))
  
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities))
  
  :on-resize
  (fn [{:keys [width height] :as screen} entities]
    (height! screen height)
    (let [c1 (color :black)
          c2 (color 79/256 90/256 100/256 1)]
      (for [e entities]
        (case (:id e)
          :background (shape e :rect 0 0 width height c1 c1 c2 c2)
          e))))
  
  :on-ui-enter
  (fn [screen entities]
    (when (some-> (:actor screen) (actor! :get-parent) image-text-button?)
      (actor! (:actor screen) :set-color (color :white))))
  
  :on-ui-exit
  (fn [screen entities]
    (when (some-> (:actor screen) (actor! :get-parent) image-text-button?)
      (actor! (:actor screen) :set-color 1 1 1 0.5))
    (if (or (some-> (:actor screen) image-text-button?)
            (some-> (:actor screen) (actor! :get-parent) image-text-button?))
      (set-cursor! (:cursor screen))
      (set-cursor! nil)))
  
  :on-ui-changed
  (fn [screen entities]
    (when-let [n (actor! (:actor screen) :get-name)]
      (sound! (:click-sound screen) :play)
      (cond
        (= n "download")
        (clone-project! (get-clipboard))
        
        (contains? (set templates) n)
        (new-project! n)
        
        :else
        (load-project! n)))
    nil))

(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(defscreen overlay-screen
  :on-show
  (fn [screen entities]
    (binding [play-clj.utils/*asset-manager* manager]
      (update! screen
               :camera (orthographic)
               :renderer (stage)
               :screenshot-sound (sound "screenshot.ogg"))
      (let [ui-skin (skin "uiskin.json")
            small-font (skin! ui-skin :get-font "small-font")
            home-style (style :image-button
                              (texture-drawable "home_up.png")
                              (texture-drawable "home_down.png")
                              nil nil nil nil)
            restart-style (style :image-button
                                 (texture-drawable "restart_up.png")
                                 (texture-drawable "restart_down.png")
                                 nil nil nil nil)
            screenshot-style (style :image-button
                                    (texture-drawable "screenshot_up.png")
                                    (texture-drawable "screenshot_down.png")
                                    nil nil nil nil)
            files-style (style :image-button
                               (texture-drawable "files_up.png")
                               (texture-drawable "files_down.png")
                               nil nil nil nil)
            docs-style (style :image-button
                              (texture-drawable "docs_up.png")
                              (texture-drawable "docs_down.png")
                              nil nil nil nil)
            repl-style (style :image-button
                              (texture-drawable "repl_up.png")
                              (texture-drawable "repl_down.png")
                              nil nil nil nil)]
        [(-> (label "" (style :label small-font (color :white))
                    :set-wrap true
                    :set-alignment (bit-or (align :left) (align :bottom)))
             (scroll-pane (style :scroll-pane nil nil nil nil nil))
             (assoc :id :text :x pad-small :y pad-small))
         (-> [(image-button home-style :set-name "home")
              (image-button restart-style :set-name "restart")
              (image-button screenshot-style :set-name "screenshot")
              (image-button files-style :set-name "files")
              (image-button docs-style :set-name "docs")
              (image-button repl-style :set-name "repl")]
             (horizontal :space (* 2 pad-small) :pack)
             (assoc :id :menu :x pad-small))
         (-> [(image-button (texture-drawable "home_key.png"))
              (image-button (texture-drawable "restart_key.png"))
              (image-button (texture-drawable "screenshot_key.png"))
              (image-button (texture-drawable "files_key.png"))
              (image-button (texture-drawable "docs_key.png"))
              (image-button (texture-drawable "repl_key.png"))]
             (horizontal :space (* 2 pad-small) :pack)
             (assoc :id :menu-keys :x pad-small))
         (-> [(text-button (nc-utils/get-string :stack-trace) ui-skin
                           :set-name "stack-trace")
              (text-button (nc-utils/get-string :copy) ui-skin
                           :set-name "copy")]
             (horizontal :space (* 2 pad-small) :pack)
             (assoc :id :error-buttons :x pad-small :y pad-small))])))
  
  :on-render
  (fn [screen entities]
    (when (:screenshot? screen)
      (take-screenshot! screen))
    (->> (for [e entities]
           (case (:id e)
             :text (do (label! (scroll-pane! e :get-widget)
                               :set-text (out-str))
                     e)
             :error-buttons (doto e (set-visible! (some? @u/error)))
             :menu (doto e (set-visible! (not @shortcuts/down?)))
             :menu-keys (doto e (set-visible! @shortcuts/down?))
             e))
         (render! screen)))
  
  :on-resize
  (fn [{:keys [width height] :as screen} entities]
    (height! screen height)
    (for [e entities]
      (case (:id e)
        :menu (assoc e :y (- height (vertical! e :get-height) pad-small))
        :menu-keys (assoc e :y (- height (vertical! e :get-height) pad-small))
        :text (let [padded-height (+ button-height (* 2 pad-small))
                    height-diff (if @u/error (* 2 padded-height) padded-height)
                    y (if @u/error padded-height pad-small)]
                (assoc e :width width :height (- height height-diff) :y y))
        e)))
  
  :on-ui-changed
  (fn [screen entities]
    (case (actor! (:actor screen) :get-name)
      "home" (home!)
      "restart" (restart!)
      "screenshot" (schedule-screenshot!)
      "files" (toggle-files!)
      "docs" (toggle-docs!)
      "repl" (toggle-repl!)
      "stack-trace" (swap! u/stack-trace? not)
      "copy" (set-clipboard! (out-str))
      nil)
    nil))

; misc

(defgame nightmod
  :on-create
  (fn [this]
    ; initialize box2d
    (try (Class/forName
           "com.badlogic.gdx.physics.box2d.World")
      (catch Exception _))
    ; initialize bullet
    @init-bullet
    ; show the main screen
    (set-screen! this main-screen)))

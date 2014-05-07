(ns nightmod.core
  (:require [cemerick.pomegranate :as pom]
            [clojure.java.io :as io]
            [nightcode.editors :as editors]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [nightcode.window :as window]
            [nightmod.git :as git]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [nightmod.sandbox :as sandbox]
            [play-clj.core :as play-clj]
            [seesaw.core :as s]
            [seesaw.util :as s-util])
  (:import [java.awt BorderLayout Canvas Dimension Window]
           [java.awt.event ComponentAdapter KeyEvent KeyListener WindowAdapter]
           [javax.swing JLayeredPane]
           [com.badlogic.gdx.backends.lwjgl LwjglApplication LwjglInput]
           [nightmod KeyCodeConverter]
           [org.lwjgl.input Keyboard])
  (:gen-class))

; allow s/select to work with Canvas
(extend-protocol s-util/Children
  java.awt.Component (children [this] nil))

(defn set-hint-container!
  "Sets the container in which the hints will be stored."
  [container]
  (intern 'nightcode.shortcuts '*hint-container* container))

(defn create-layered-pane
  "Returns the layered pane holding the editor pane."
  []
  (let [layered-pane (doto (JLayeredPane.) set-hint-container!)
        pane (editors/create-pane)]
    (doto layered-pane
      (.setPreferredSize (Dimension. u/editor-width u/window-height))
      (.addComponentListener (proxy [ComponentAdapter] []
                               (componentResized [e]
                                 (->> (.getComponent e)
                                      .getHeight
                                      (.setBounds pane 0 0 u/editor-width)))))
      (.add pane))))

(defn awt->gdx
  "Translates key code from AWT to LibGDX."
  [keycode]
  (-> keycode KeyCodeConverter/translateFromAWT LwjglInput/getGdxKeyCode))

(defn pass-key-events!
  "Passes key events to the game."
  [window game]
  (.addKeyListener
    window
    (reify KeyListener
      (keyReleased [this e]
        (-> game
            .getInput
            .getInputProcessor
            (.keyUp (awt->gdx (.getKeyCode e)))
            play-clj/on-gl))
      (keyTyped [this e]
        (-> game
            .getInput
            .getInputProcessor
            (.keyTyped (.getKeyChar e))
            play-clj/on-gl))
      (keyPressed [this e]
        (-> game
            .getInput
            .getInputProcessor
            (.keyDown (awt->gdx (.getKeyCode e)))
            play-clj/on-gl)))))

(defn override-save-button!
  "Makes the editor save button restart the game."
  []
  (let [orig-save-file! editors/save-file!]
    (intern 'nightcode.editors
            'save-file!
            (fn [& _]
              (orig-save-file!)
              (screens/restart!)
              true))))

(defn show-internal-editor!
  "Shows the internal editor."
  [main-window editor-window]
  (let [editor-pane (ui/get-editor-pane)
        l-pane (-> main-window .getGlassPane (s/select [:JLayeredPane]) first)]
    (reset! ui/root main-window)
    (u/toggle-glass! true)
    (.add l-pane editor-pane)
    (set-hint-container! l-pane)
    (s/hide! editor-window)))

(defn show-external-editor!
  "Shows the external editor."
  [main-window editor-window]
  (let [editor-pane (ui/get-editor-pane)]
    (u/toggle-glass! false)
    (reset! ui/root editor-window)
    (s/config! editor-window :content editor-pane)
    (set-hint-container! (.getLayeredPane editor-window))
    (s/show! editor-window)))

(defn adjust-widgets!
  "Adds and removes widgets from the window."
  [main-window]
  (let [external? (atom false)
        editor-window (s/frame :width 800 :height 600 :on-close :hide)
        toggle-window! (fn [& _]
                         (if (swap! external? not)
                           (show-external-editor! main-window editor-window)
                           (show-internal-editor! main-window editor-window)))
        window-btn (ui/button :id :window
                              :text (nc-utils/get-string :toggle-window)
                              :focusable? false
                              :listen [:action toggle-window!])]
    (.addWindowListener editor-window
      (proxy [WindowAdapter] []
        (windowClosing [e]
          (toggle-window!))))
    (intern 'nightcode.editors
            '*widgets*
            [:up :save :undo :redo :font-dec :font-inc
             :doc :paredit :paredit-help :close])
    (intern 'nightcode.file-browser
            '*widgets*
            [:up :new-file :edit :open-in-browser :save :cancel window-btn])))

(defn protect-file!
  "Prevents renaming or deleting a file."
  [path]
  (intern 'nightcode.file-browser
          'protect-file?
          #(= % path)))

(defn load-game!
  "Loads game into the canvas and runs it in a sandbox."
  [path]
  (pom/add-classpath path)
  (doto (.getCanonicalPath (io/file path "core.clj"))
    protect-file!
    sandbox/run-file!))

(defn create-window
  "Creates the main window."
  []
  (s/frame :title (str "Nightmod " (or (some-> "nightmod.core"
                                               nc-utils/get-project
                                               (nth 2))
                                       "beta"))
           :width u/window-width
           :height u/window-height
           :on-close :nothing))

(defn init-window
  "Adds content and listeners to the main window."
  [window canvas]
  (doto window
    ; add canvas and editor pane
    (-> .getContentPane (doto
                          (.add (s/border-panel :center canvas
                                                :focusable? false))))
    (-> .getGlassPane (doto
                        (.setLayout (BorderLayout.))
                        (.add (create-layered-pane) BorderLayout/EAST)
                        (.setVisible false)))
    ; set various window properties
    window/enable-full-screen!
    window/add-listener!))

(defn -main
  "Launches the main window."
  [& args]
  (sandbox/set-policy!)
  (window/set-icon! "logo_launcher.png")
  (window/set-theme! args)
  (add-watch u/project-dir
             :load-game
             (fn [_ _ _ path]
               (load-game! path)))
  (s/invoke-now
    ; listen for keys while modifier is down
    (shortcuts/listen-for-shortcuts!
      (fn [key-code]
        (case key-code
          ; page up
          33 (editors/move-tab-selection! -1)
          ; page down
          34 (editors/move-tab-selection! 1)
          ; Q
          81 (window/confirm-exit-app!)
          ; W
          87 (editors/close-selected-editor!)
          ; else
          false)))
    ; create the window
    (let [window (create-window)
          canvas (doto (Canvas.) (.setFocusable false))]
      (override-save-button!)
      (adjust-widgets! window)
      (s/show! (reset! ui/root (init-window window canvas)))
      (->> (LwjglApplication. screens/nightmod canvas)
           (pass-key-events! window))))
  (Keyboard/enableRepeatEvents true))

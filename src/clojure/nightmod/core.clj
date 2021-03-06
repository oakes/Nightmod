(ns nightmod.core
  (:require [clojure.core.logic]
            [clojure.java.io :as io]
            [nightcode.editors :as editors]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [nightcode.window :as window]
            [nightmod.sandbox :as sandbox]
            [nightmod.screens :as screens]
            [nightmod.input :as input]
            [nightmod.overlay :as overlay]
            [nightmod.utils :as u]
            [schema.core]
            [seesaw.core :as s]
            [seesaw.util :as s-util])
  (:import [java.awt BorderLayout Canvas]
           [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard]
           [javax.swing UIManager]
           [org.pushingpixels.substance.api.skin SubstanceGraphiteLookAndFeel])
  (:gen-class))

; allow s/select to work with Canvas
(extend-protocol s-util/Children
  java.awt.Component (children [this] nil))

(defn load-game!
  "Loads game into the canvas and runs it."
  [path]
  (doto (.getCanonicalPath (io/file path u/core-file))
    overlay/protect-file!
    sandbox/run-file!))

(defn create-window
  "Creates the main window."
  []
  (s/frame :title (str "Nightmod 1.3.4")
           :width u/window-width
           :height u/window-height
           :on-close :nothing))

(defn init-window
  "Adds content and listeners to the main window."
  [window canvas]
  (doto window
    ; add canvas and editor pane
    (-> .getContentPane (doto
                          (.setLayout (BorderLayout.))
                          (.add (s/border-panel :center canvas
                                                :focusable? (u/canvas-focus?))
                            BorderLayout/CENTER)))
    ; set various window properties
    window/enable-full-screen!
    window/add-listener!))

(defn start-app!
  "Displays the window and all its contents."
  []
  (let [canvas (doto (Canvas.) (.setFocusable (u/canvas-focus?)))
        window (s/show! (init-window (create-window) canvas))
        app (LwjglApplication. screens/nightmod canvas)]
    (overlay/override-save-button!)
    (overlay/enable-toggling! window)
    (reset! u/editor (overlay/create-editor-pane))
    (reset! ui/root window)
    (when-not (u/canvas-focus?)
      (input/pass-key-events! window app))))

(defn set-theme!
  "Sets the theme based on the command line arguments."
  []
  (s/native!)
  (UIManager/setLookAndFeel (SubstanceGraphiteLookAndFeel.)))

(defn -main
  "Launches the main window."
  [& args]
  (window/set-icon! "logo_launcher.png")
  (add-watch u/project-dir :load-game (fn [_ _ _ path]
                                        (load-game! path)))
  (->> (u/get-data-dir) (reset! u/main-dir) io/file .mkdir)
  (s/invoke-later
    (set-theme!)
    ; listen for keys while modifier is down
    (shortcuts/listen-for-shortcuts!
      (fn [key-code]
        (case key-code
          ; page up
          33 (editors/move-tab-selection! -1)
          ; page down
          34 (editors/move-tab-selection! 1)
          ; B
          66 (do (screens/home!) true)
          ; D
          68 (do (screens/toggle-files!) true)
          ; E
          69 (do (screens/toggle-repl!) true)
          ; O
          79 (do (screens/toggle-docs!) true)
          ; Q
          81 (window/confirm-exit-app!)
          ; R
          82 (do (screens/restart!) true)
          ; T
          84 (do (screens/schedule-screenshot!) true)
          ; W
          87 (if (some-> @ui/tree-selection io/file .exists)
               (editors/close-selected-editor!)
               false)
          ; else
          false)))
    ; create the window
    (start-app!))
  (Keyboard/enableRepeatEvents true))

(ns nightmod.core
  (:require [clojure.java.io :as io]
            [nightcode.editors :as editors]
            [nightcode.sandbox :as nc-sandbox]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [nightcode.window :as window]
            [nightmod.git :as git]
            [nightmod.manager :as manager]
            [nightmod.sandbox :as sandbox]
            [nightmod.screens :as screens]
            [nightmod.input :as input]
            [nightmod.overlay :as overlay]
            [nightmod.utils :as u]
            [seesaw.core :as s]
            [seesaw.util :as s-util])
  (:import [java.awt BorderLayout Canvas]
           [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

; allow s/select to work with Canvas
(extend-protocol s-util/Children
  java.awt.Component (children [this] nil))

(defn load-game!
  "Loads game into the canvas and runs it in a sandbox."
  [path]
  (manager/clean!)
  (doto (.getCanonicalPath (io/file path u/first-file))
    overlay/protect-file!
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
                        (.add (overlay/create-layered-pane)
                          BorderLayout/EAST)
                        (.setVisible false)))
    ; set various window properties
    window/enable-full-screen!
    window/add-listener!))

(defn -main
  "Launches the main window."
  [& args]
  (window/set-icon! "logo_launcher.png")
  (window/set-theme! args)
  (nc-sandbox/set-home!)
  (sandbox/set-policy!)
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
      (overlay/override-save-button!)
      (overlay/adjust-widgets! window)
      (s/show! (reset! ui/root (init-window window canvas)))
      (->> (LwjglApplication. screens/nightmod canvas)
           (input/pass-key-events! window))))
  (Keyboard/enableRepeatEvents true))

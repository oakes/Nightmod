(ns nightmod.core.desktop-launcher
  (:require [clojure.java.io :as io]
            [nightmod.core :refer :all]
            [nightcode.editors :as editors]
            [nightcode.dialogs :as dialogs]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.utils :as utils]
            [nightcode.window :as window]
            [seesaw.core :as s]
            [seesaw.util :as s-util])
  (:import [java.awt BorderLayout Canvas Dimension Window]
           [java.awt.event ComponentAdapter WindowAdapter]
           [javax.swing JLayeredPane]
           [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard]
           [org.pushingpixels.substance.api SubstanceLookAndFeel]
           [org.pushingpixels.substance.api.skin GraphiteSkin])
  (:gen-class))

; allow s/select to work with Canvas
(extend-protocol s-util/Children
  java.awt.Component (children [this] nil))

(def ^:const window-width 1200)
(def ^:const window-height 768)
(def ^:const editor-width 700)

(def editor-widgets [:save :undo :redo :font-dec :font-inc
                     :doc :paredit :paredit-help])

(defn create-layered-pane
  "Returns the pane with the editors."
  []
  (let [pane (editors/create-pane)]
    (doto (JLayeredPane.)
      (.setPreferredSize (Dimension. editor-width window-height))
      (.addComponentListener (proxy [ComponentAdapter] []
                               (componentResized [e]
                                 (->> (.getComponent e)
                                      .getHeight
                                      (.setBounds pane 0 0 editor-width)))))
      (.add pane))))

(defn create-canvas-pane
  "Returns the pane with the canvas."
  []
  (let [canvas (Canvas.)]
    (LwjglApplication. nightmod true canvas)
    canvas))

(defn create-window
  "Creates the main window."
  []
  (doto (s/frame :title (str (utils/get-string :app_name)
                             " "
                             (if-let [p (utils/get-project
                                          "nightmod.core.desktop_launcher")]
                               (nth p 2)
                               "beta"))
                 :width window-width
                 :height window-height
                 :on-close :nothing)
    ; add canvas and editor pane
    (-> .getContentPane (.add (create-canvas-pane)))
    (-> .getGlassPane (doto
                        (.setLayout (BorderLayout.))
                        (.add (create-layered-pane) BorderLayout/EAST)
                        (.setVisible true)))
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
          ;87 (editors/close-selected-editor!)
          ; else
          false)))
    ; set various window properties
    window/enable-full-screen!
    window/add-listener!))

(defn -main
  "Launches the main window."
  [& args]
  (window/set-theme! args)
  (s/invoke-later
    (s/show! (reset! ui/ui-root (create-window)))
    (binding [editors/*editor-widgets* editor-widgets]
      (comment editors/show-editor! "")))
  (Keyboard/enableRepeatEvents true))

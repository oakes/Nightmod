(ns nightmod.core.desktop-launcher
  (:require [clojure.java.io :as io]
            [nightmod.core :refer :all]
            [nightmod.core.editors :as editors]
            [nightmod.core.dialogs :as dialogs]
            [nightmod.core.shortcuts :as shortcuts]
            [nightmod.core.ui :as ui]
            [nightmod.core.utils :as utils]
            [seesaw.core :as s])
  (:import [java.awt BorderLayout Canvas Dimension]
           [java.awt.event ComponentAdapter WindowAdapter]
           [javax.swing JLayeredPane]
           [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.fife.ui.rsyntaxtextarea FileLocation SyntaxConstants
            TextEditorPane Theme]
           [org.lwjgl.input Keyboard]
           [org.pushingpixels.substance.api SubstanceLookAndFeel]
           [org.pushingpixels.substance.api.skin GraphiteSkin])
  (:gen-class))

(def ^:const window-width 1200)
(def ^:const window-height 768)
(def ^:const editor-width 700)

(defn create-layered-pane
  "Returns the pane with the editors."
  []
  (let [pane (s/card-panel :id :editor-pane :items [["" :default-card]])]
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

(defn confirm-exit-app!
  "Displays a dialog confirming whether the program should shut down."
  []
  (if (dialogs/show-shut-down-dialog!)
    (System/exit 0)
    true))

(defn create-window
  "Creates the main window."
  []
  (doto (s/frame :title (str (utils/get-string :app_name)
                             " "
                             (utils/get-version))
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
          81 (confirm-exit-app!)
          ; W
          ;87 (editors/close-selected-editor!)
          ; else
          false)))
    ; when the window state changes
    (.addWindowListener (proxy [WindowAdapter] []
                          (windowClosing [e]
                            (confirm-exit-app!))))))

(defn -main
  "Launches the main window."
  []
  (s/native!)
  (SubstanceLookAndFeel/setSkin (GraphiteSkin.))
  (s/invoke-later
    (s/show! (reset! ui/ui-root (create-window))))
  (Keyboard/enableRepeatEvents true))

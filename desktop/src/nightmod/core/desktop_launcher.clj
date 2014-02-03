(ns nightmod.core.desktop-launcher
  (:require [clojure.java.io :as io]
            [nightmod.core :refer :all]
            [seesaw.core :as s])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [java.awt BorderLayout Canvas Color Panel]
           [org.fife.ui.rsyntaxtextarea FileLocation SyntaxConstants
            TextEditorPane Theme]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn set-theme!
  [text-area path]
  (-> path
      io/resource
      io/input-stream
      Theme/load
      (.apply text-area))
  (when-let [c (.getBackground text-area)]
    (.setBackground text-area
      (Color. (.getRed c) (.getGreen c) (.getBlue c) 128))))

(defn create-text-area
  ([]
    (doto (proxy [TextEditorPane] []
            (setMarginLineEnabled [is-enabled?]
              (proxy-super setMarginLineEnabled is-enabled?))
            (setMarginLinePosition [size]
              (proxy-super setMarginLinePosition size))
            (processKeyBinding [ks e condition pressed]
              (proxy-super processKeyBinding ks e condition pressed)))
      (.setAntiAliasingEnabled true)
      (set-theme! "dark.xml")
      (.setOpaque false)))
  ([path]
    (doto (create-text-area)
      (.load (FileLocation/create path) nil)
      .discardAllEdits
      (.setSyntaxEditingStyle SyntaxConstants/SYNTAX_STYLE_CLOJURE)
      (.setLineWrap false)
      (.setMarginLineEnabled true)
      (.setMarginLinePosition 80)
      (.setTabSize 2))))

(defn create-canvas
  []
  (let [canvas (Canvas.)]
    (LwjglApplication. nightmod true canvas)
    canvas))

(defn -main
  []
  (s/invoke-later
    (doto (s/frame :title "Nightmod"
                   :width 800
                   :height 600
                   :on-close :exit)
      (-> .getContentPane (.add (create-canvas)))
      (-> .getGlassPane (doto
                          (.setLayout (BorderLayout.))
                          (.add (create-text-area) BorderLayout/CENTER)
                          (.setVisible true)))
      s/show!))
  (Keyboard/enableRepeatEvents true))

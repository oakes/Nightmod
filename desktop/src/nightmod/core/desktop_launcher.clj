(ns nightmod.core.desktop-launcher
  (:require [clojure.java.io :as io]
            [nightmod.core :refer :all]
            [seesaw.core :as s])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [java.awt BorderLayout Canvas Dimension]
           [org.fife.ui.rsyntaxtextarea FileLocation SyntaxConstants
            TextEditorPane Theme]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(def ^:const window-width 1200)
(def ^:const window-height 768)
(def ^:const editor-width (/ window-width 2))

(defn set-theme!
  [text-area path]
  (-> path
      io/resource
      io/input-stream
      Theme/load
      (.apply text-area)))

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
      (.setPreferredSize (Dimension. editor-width window-height))))
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
                   :width window-width
                   :height window-height
                   :on-close :exit)
      (-> .getContentPane (.add (create-canvas)))
      (-> .getGlassPane (doto
                          (.setLayout (BorderLayout.))
                          (.add (create-text-area) BorderLayout/EAST)
                          (.setVisible true)))
      s/show!))
  (Keyboard/enableRepeatEvents true))

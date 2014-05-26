(ns nightmod.input
  (:require [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightmod.utils :as u]
            [play-clj.core :as play-clj])
  (:import [java.awt KeyboardFocusManager KeyEventDispatcher]
           [java.awt.event ComponentAdapter KeyEvent]
           [com.badlogic.gdx.backends.lwjgl LwjglInput]
           [nightmod KeyCodeConverter]
           [org.lwjgl.input Keyboard]
           [org.lwjgl.opengl InputImplementation]))

(defn awt->lwjgl
  "Translates key code from AWT to LWJGL."
  [key]
  (KeyCodeConverter/translateFromAWT key))

(defn awt->libgdx
  "Translates key code from AWT to LibGDX."
  [key]
  (LwjglInput/getGdxKeyCode (awt->lwjgl key)))

(defn pass-key-events!
  "Passes key events to the game."
  [window game]
  (let [key-buf-field (doto (.getDeclaredField Keyboard "keyDownBuffer")
                        (.setAccessible true))
        key-buf (.get key-buf-field nil)
        impl-field (doto (.getDeclaredField Keyboard "implementation")
                        (.setAccessible true))
        impl (proxy [InputImplementation] []
               (pollKeyboard [bb])
               (readKeyboard [bb]))]
    (.addKeyEventDispatcher
      (KeyboardFocusManager/getCurrentKeyboardFocusManager)
      (proxy [KeyEventDispatcher] []
        (dispatchKeyEvent [^KeyEvent e]
          (cond
            ; a key was released in a game
            (and @ui/tree-selection
                 (shortcuts/focused-window? window)
                 (not (.isVisible @u/editor))
                 (= (.getID e) KeyEvent/KEY_RELEASED))
            (do
              (.set impl-field nil impl)
              (.put key-buf (awt->lwjgl (.getKeyCode e)) (byte 0))
              (-> game
                  .getInput
                  .getInputProcessor
                  (.keyUp (awt->libgdx (.getKeyCode e)))
                  play-clj/on-gl)
              true)
            ; a key was typed in a game
            (and @ui/tree-selection
                 (shortcuts/focused-window? window)
                 (not (.isVisible @u/editor))
                 (= (.getID e) KeyEvent/KEY_TYPED))
            (do
              (-> game
                  .getInput
                  .getInputProcessor
                  (.keyTyped (.getKeyChar e))
                  play-clj/on-gl)
              true)
            ; a key was pressed in a game
            (and @ui/tree-selection
                 (shortcuts/focused-window? window)
                 (not (.isVisible @u/editor))
                 (= (.getID e) KeyEvent/KEY_PRESSED))
            (do
              (.set impl-field nil impl)
              (.put key-buf (awt->lwjgl (.getKeyCode e)) (byte 8))
              (-> game
                  .getInput
                  .getInputProcessor
                  (.keyDown (awt->libgdx (.getKeyCode e)))
                  play-clj/on-gl)
              true)
            ; don't handle
            :else
            false))))))

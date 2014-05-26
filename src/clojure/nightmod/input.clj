(ns nightmod.input
  (:require [nightcode.shortcuts :as shortcuts]
            [play-clj.core :as play-clj])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglInput]
           [java.awt.event KeyEvent KeyListener]
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
    (.addKeyListener
      window
      (reify KeyListener
        (keyReleased [this e]
          ; clear the LWJGL key buffer
          ; when the OS X command key is lifted, we must clear it
          ; completely because it seems to swallow keyReleased events
          (.set impl-field nil impl)
          (if (= (.getKeyCode e) KeyEvent/VK_META)
            (doseq [i (range (.capacity key-buf))]
              (.put key-buf i (byte 0)))
            (.put key-buf (awt->lwjgl (.getKeyCode e)) (byte 0)))
          ; pass the event to LibGDX
          (-> game
              .getInput
              .getInputProcessor
              (.keyUp (awt->libgdx (.getKeyCode e)))
              play-clj/on-gl))
        (keyTyped [this e]
          ; pass the event to LibGDX
          (-> game
              .getInput
              .getInputProcessor
              (.keyTyped (.getKeyChar e))
              play-clj/on-gl))
        (keyPressed [this e]
          ; add to the LWJGL key buffer
          (.set impl-field nil impl)
          (.put key-buf (awt->lwjgl (.getKeyCode e)) (byte 1))
          ; pass the event to LibGDX
          (-> game
              .getInput
              .getInputProcessor
              (.keyDown (awt->libgdx (.getKeyCode e)))
              play-clj/on-gl))))))

(ns nightmod.input
  (:require [play-clj.core :as play-clj])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglInput]
           [java.awt.event KeyEvent KeyListener]
           [nightmod KeyCodeConverter]
           [org.lwjgl.input Keyboard]
           [org.lwjgl.opengl InputImplementation]))

(def ^:const meta-keys #{KeyEvent/VK_CONTROL KeyEvent/VK_META})

(defn awt->lwjgl
  "Translates key code from AWT to LWJGL."
  [key]
  (KeyCodeConverter/translateFromAWT key))

(defn awt->libgdx
  "Translates key code from AWT to libGDX."
  [key]
  (LwjglInput/getGdxKeyCode (awt->lwjgl key)))

(defn clear-key-buffer!
  "Clears the LWJGL key down buffer."
  ([]
    (let [key-buf-field (doto (.getDeclaredField Keyboard "keyDownBuffer")
                          (.setAccessible true))
          key-buf (.get key-buf-field nil)]
      (clear-key-buffer! key-buf)))
  ([key-buf]
    (doseq [i (range (.capacity key-buf))]
      (.put key-buf i (byte 0)))))

(defn pass-key-events!
  "Passes key events to the game."
  [window game]
  (let [key-buf-field (doto (.getDeclaredField Keyboard "keyDownBuffer")
                        (.setAccessible true))
        key-buf (.get key-buf-field nil)
        impl-field (doto (.getDeclaredField Keyboard "implementation")
                        (.setAccessible true))
        impl (proxy [InputImplementation] []
               (destroyKeyboard [])
               (pollKeyboard [bb])
               (readKeyboard [bb]))]
    (.addKeyListener
      window
      (reify KeyListener
        (keyReleased [this e]
          ; clear the LWJGL key buffer
          ; when a meta key is lifted, we must clear it completely
          ; to prevent it from getting stuck in the down position
          (.set impl-field nil impl)
          (if (contains? meta-keys (.getKeyCode e))
            (clear-key-buffer! key-buf)
            (.put key-buf (awt->lwjgl (.getKeyCode e)) (byte 0)))
          ; pass the event to libGDX
          (-> game
              .getInput
              .getInputProcessor
              (.keyUp (awt->libgdx (.getKeyCode e)))
              play-clj/on-gl))
        (keyTyped [this e]
          ; pass the event to libGDX
          (-> game
              .getInput
              .getInputProcessor
              (.keyTyped (.getKeyChar e))
              play-clj/on-gl))
        (keyPressed [this e]
          ; add to the LWJGL key buffer
          (.set impl-field nil impl)
          (.put key-buf (awt->lwjgl (.getKeyCode e)) (byte 1))
          ; pass the event to libGDX
          (-> game
              .getInput
              .getInputProcessor
              (.keyDown (awt->libgdx (.getKeyCode e)))
              play-clj/on-gl))))))

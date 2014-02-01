(ns nightmod.core.desktop-launcher
  (:require [nightmod.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. nightmod "nightmod" 800 600 true)
  (Keyboard/enableRepeatEvents true))

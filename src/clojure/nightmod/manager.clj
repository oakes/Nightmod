(ns nightmod.manager
  (:require [clojure.java.io :as io]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.utils :refer :all])
  (:import [com.badlogic.gdx.assets.loaders FileHandleResolver]
           [com.badlogic.gdx.files FileHandle]))

; make all assets load relative to the current project's directory
(def manager (asset-manager*
               (reify FileHandleResolver
                 (resolve [this file-name]
                   (FileHandle. (io/file @u/project-dir file-name))))))
(set-asset-manager! manager)

; keep a reference to all timers to we can stop them later
(track-timers!)

(defn clean!
  []
  (remove-ns u/game-ns)
  (stop-timers!)
  (on-gl (asset-manager! manager :clear)))

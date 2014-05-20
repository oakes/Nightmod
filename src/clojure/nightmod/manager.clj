(ns nightmod.manager
  (:require [clojure.java.io :as io]
            [nightmod.utils :as u]
            [play-clj.core :refer :all])
  (:import [com.badlogic.gdx.assets.loaders FileHandleResolver]
           [com.badlogic.gdx.files FileHandle]))

; make all assets load relative to the current project's directory
(def manager (asset-manager*
               (reify FileHandleResolver
                 (resolve [this file-name]
                   (FileHandle. (io/file @u/project-dir file-name))))))
(set-asset-manager! manager)

(defn clear-ns!
  [nspace]
  (doall (map #(ns-unmap nspace %) (keys (ns-interns nspace)))))

(defn clean!
  []
  (clear-ns! u/game-ns)
  (on-gl (asset-manager! manager :clear)))

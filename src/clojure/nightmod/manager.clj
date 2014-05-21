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

; keep a reference to all timers to we can stop them later
(def timers (atom []))
(let [create-and-add-timer! (deref #'play-clj.core/create-and-add-timer!)]
  (intern 'play-clj.core
          'create-and-add-timer!
          (fn [screen id]
            (let [t (create-and-add-timer! screen id)]
              (swap! timers conj t)
              t))))

(defn stop-timers!
  []
  (doseq [t @timers]
    (.stop t))
  (reset! timers []))

(defn clear-ns!
  [nspace]
  (doall (map #(ns-unmap nspace %) (keys (ns-interns nspace)))))

(defn clean!
  []
  (clear-ns! u/game-ns)
  (stop-timers!)
  (on-gl (asset-manager! manager :clear)))

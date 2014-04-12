(ns nightmod.overlay
  (:require [nightcode.editors :as editors]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightmod.utils :as u]
            [seesaw.core :as s]))

(defn show-home!
  [& _]
  (reset! ui/tree-selection @u/project-dir))

(defn create-home-button
  []
  (s/button :id :home
            :text (shortcuts/wrap-hint-text "&larr;")
            :focusable? false
            :listen [:action show-home!]))

(defn select-file!
  [path]
  (binding [editors/*widgets* [(create-home-button)
                               :save :undo :redo :font-dec :font-inc
                               :doc :paredit :paredit-help :close]]
    (reset! ui/tree-selection path)))

(ns nightmod.core.overlay
  (:require [nightcode.editors :as editors]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [seesaw.core :as s]))

(defn show-home!
  [& _]
  (reset! ui/tree-selection nil))

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

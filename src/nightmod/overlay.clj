(ns nightmod.overlay
  (:require [nightcode.dialogs :as dialogs]
            [nightcode.editors :as editors]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.utils :as code-utils]
            [seesaw.core :as s]))

(defn new-file!
  [& _]
  (when (dialogs/show-file-path-dialog! nil)))

(defn create-home-widgets
  []
  [(ui/button :id :new-file
              :text (code-utils/get-string :new_file)
              :listen [:action new-file!]
              :focusable? false)])

(defn create-home-card
  []
  (s/border-panel :north (ui/wrap-panel :items (create-home-widgets))
                  :center (s/scrollable (s/grid-panel :columns 5))))

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

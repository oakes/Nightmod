(ns nightmod.overlay
  (:require [nightmod.docs :as docs]
            [nightmod.repl :as repl]
            [nightcode.editors :as editors]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [seesaw.core :as s])
  (:import [java.awt Dimension]
           [java.awt.event ComponentAdapter WindowAdapter]
           [javax.swing JLayeredPane]))

(defn set-hint-container!
  "Sets the container in which the hints will be stored."
  [container]
  (intern 'nightcode.shortcuts '*hint-container* container))

(defn create-layered-pane
  "Returns the layered pane holding the editor pane."
  []
  (let [layered-pane (doto (JLayeredPane.) set-hint-container!)
        pane (doto (editors/create-pane)
               (.add (docs/create-card) u/docs-name)
               (.add (repl/create-card) u/repl-name))]
    (doto layered-pane
      (.setPreferredSize (Dimension. u/editor-width u/window-height))
      (.addComponentListener (proxy [ComponentAdapter] []
                               (componentResized [e]
                                 (->> (.getComponent e)
                                      .getHeight
                                      (.setBounds pane 0 0 u/editor-width)))))
      (.add pane))))

(defn override-save-button!
  "Makes the editor save button restart the game."
  []
  (let [orig-save-file! editors/save-file!]
    (intern 'nightcode.editors
            'save-file!
            (fn [& _]
              (orig-save-file!)
              (screens/restart!)
              true))))

(defn show-internal-editor!
  "Shows the internal editor."
  [main-window editor-window]
  (let [editor-pane (ui/get-editor-pane)
        l-pane (-> main-window .getGlassPane (s/select [:JLayeredPane]) first)]
    (reset! ui/root main-window)
    (when @ui/tree-selection
      (u/toggle-glass! true))
    (.add l-pane editor-pane)
    (set-hint-container! l-pane)
    (s/hide! editor-window)))

(defn show-external-editor!
  "Shows the external editor."
  [main-window editor-window]
  (let [editor-pane (ui/get-editor-pane)]
    (u/toggle-glass! false)
    (reset! ui/root editor-window)
    (s/config! editor-window :content editor-pane)
    (set-hint-container! (.getLayeredPane editor-window))
    (s/show! editor-window)))

(defn adjust-widgets!
  "Adds and removes widgets from the window."
  [main-window]
  (let [external? (atom false)
        editor-window (s/frame :width 800 :height 600 :on-close :hide)
        toggle-window! (fn [& _]
                         (if (swap! external? not)
                           (show-external-editor! main-window editor-window)
                           (show-internal-editor! main-window editor-window)))
        window-btn #(ui/button :id :window
                               :text (nc-utils/get-string :toggle-window)
                               :listen [:action toggle-window!])]
    (.addWindowListener editor-window
      (proxy [WindowAdapter] []
        (windowClosing [e]
          (toggle-window!))))
    (intern 'nightcode.editors
            '*widgets*
            [:up :save :undo :redo :font-dec :font-inc
             :doc :paredit :paredit-help :close])
    (intern 'nightcode.file-browser
            '*widgets*
            [:up :new-file :edit :open-in-browser :save :cancel (window-btn)])
    (intern 'nightmod.docs
            '*widgets*
            [(window-btn)])
    (intern 'nightmod.repl
            '*widgets*
            [:restart (window-btn)])))

(defn protect-file!
  "Prevents renaming or deleting a file."
  [path]
  (intern 'nightcode.file-browser
          'protect-file?
          #(= % path)))

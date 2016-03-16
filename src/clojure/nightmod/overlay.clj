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

(defn create-editor-pane
  "Returns the editor pane."
  []
  (doto (editors/create-pane)
    (.setVisible false)
    (.add (docs/create-card) u/docs-name)
    (.add (repl/create-card) u/repl-name)
    (.setPreferredSize (Dimension. u/editor-width 0))))

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
  (u/toggle-editor! false)
  (reset! ui/root main-window)
  (u/toggle-editor! (some? @ui/tree-selection))
  (set-hint-container! (.getLayeredPane main-window))
  (s/hide! editor-window))

(defn show-external-editor!
  "Shows the external editor."
  [main-window editor-window]
  (u/toggle-editor! false)
  (reset! ui/root editor-window)
  (u/toggle-editor! (some? @ui/tree-selection))
  (set-hint-container! (.getLayeredPane editor-window))
  (s/show! editor-window))

(defn enable-toggling!
  "Allows the editor pane to be toggled in and out of the main window."
  [main-window]
  (let [external? (atom false)
        editor-window (s/frame :id :ext :width 800 :height 600 :on-close :hide)
        toggle-window! (fn [& _]
                         (if (swap! external? not)
                           (show-external-editor! main-window editor-window)
                           (show-internal-editor! main-window editor-window)))
        window-btn #(s/button :id :window
                              :text (nc-utils/get-string :toggle-window)
                              :listen [:action toggle-window!])]
    (.addWindowListener editor-window (proxy [WindowAdapter] []
                                        (windowClosing [e]
                                          (toggle-window!))))
    (intern 'nightcode.ui 'get-editor-pane (fn [] @u/editor))
    (intern 'nightcode.editors '*widgets* [:up :save :undo :redo
                                           :font-dec :font-inc
                                           :find :close])
    (intern 'nightcode.file-browser '*widgets* [:up :new-file :edit
                                                :open-in-browser :save :cancel
                                                (window-btn)])
    (intern 'nightmod.docs '*widgets* [(window-btn)])
    (intern 'nightmod.repl '*widgets* [:restart (window-btn)])))

(defn protect-file!
  "Prevents renaming or deleting a file."
  [path]
  (intern 'nightcode.file-browser
          'protect-file?
          #(= % path)))

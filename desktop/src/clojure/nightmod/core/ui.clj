(ns nightmod.core.ui
  (:require [clojure.java.io :as io]
            [seesaw.core :as s]
            [seesaw.util :as s-util])
  (:import [com.camick WrapLayout]
           [java.awt Dimension FontMetrics]
           [javax.swing JComponent]))

(extend-protocol s-util/Children
  java.awt.Component (children [this] nil))

(def ui-root (atom nil))

(defn wrap-panel
  "Returns a panel based on FlowLayout that allows its contents to wrap."
  [& {:keys [items align hgap vgap]}]
  (let [align (case align
                :left WrapLayout/LEFT
                :center WrapLayout/CENTER
                :right WrapLayout/RIGHT
                :leading WrapLayout/LEADING
                :trailing WrapLayout/TRAILING
                WrapLayout/LEFT)
        hgap (or hgap 0)
        vgap (or vgap 0)
        panel (s/abstract-panel (WrapLayout. align hgap vgap) {})]
    (doseq [item items]
      (s/add! panel item))
    panel))

(defn adjust-button!
  "Adjusts the given button to fit its contents."
  [^JComponent btn]
  (let [width (-> (.getFontMetrics btn (.getFont btn))
                  (.getStringBounds (.getText btn) (.getGraphics btn))
                  .getWidth
                  (+ 30))
        height (-> btn .getPreferredSize .getHeight)]
    (doto btn
      (.setPreferredSize (Dimension. width height)))))

(defn config!
  "Sets a widget's property if necessary."
  [pane id k v]
  (when-let [widget (s/select pane [id])]
    (when (not= v (s/config widget k))
      (s/config! widget k v)
      true)))

(defmacro button
  "Creates an adjusted button."
  [& body]
  `(adjust-button! (s/button ~@body)))

(defmacro toggle
  "Creates an adjusted toggle."
  [& body]
  `(adjust-button! (s/toggle ~@body)))

(defn get-editor-pane
  "Returns the editor pane."
  []
  (s/select @ui-root [:#editor-pane]))

(defn get-layered-pane
  "Returns the JLayeredPane holding the editor pane."
  []
  (first (s/select (.getGlassPane @ui-root) [:JLayeredPane])))

(def selection (atom nil))

(defn get-selected-path
  "Returns the path selected in the project tree."
  []
  @selection)

(ns nightmod.docs
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [nightcode.editors :as editors]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [nightmod.sandbox :as sandbox]
            [seesaw.core :as s])
  (:import [javax.swing.event TreeSelectionListener]
           [javax.swing.tree DefaultMutableTreeNode DefaultTreeModel
            TreeSelectionModel]))

(defn get-groups
  [{:keys [ns groups]}]
  (let [ns (when (> (count ns) 0) ns)]
    (map #(assoc % :ns (or ns "play-clj.core")) groups)))

(defn blacklisted?
  [{:keys [name]}]
  (contains? sandbox/blacklist-symbols (symbol name)))

(def doc-list (->> (io/resource "doc.edn")
                   slurp
                   edn/read-string
                   (map get-groups)
                   (apply concat)
                   (remove blacklisted?)))

(def doc-map (into {} doc-list))

(def ns-list (vec (group-by :ns doc-list)))

(defn update-content!
  [node]
  (let [content (s/select @ui/root [:#docs-content])]
    (doto content
      (.setText (:docstring node))
      (.setCaretPosition 0))))

(defn var-node
  [{:keys [ns name] :as node}]
  (proxy [DefaultMutableTreeNode] [node]
    (isLeaf [] true)
    (toString [] name)))

(defn ns-node
  [[ns items]]
  (proxy [DefaultMutableTreeNode] []
    (getChildAt [i] (var-node (nth items i)))
    (getChildCount [] (count items))
    (toString [] ns)))

(defn root-node
  []
  (proxy [DefaultMutableTreeNode] []
    (getChildAt [i] (ns-node (nth ns-list i)))
    (getChildCount [] (count ns-list))))

(defn create-sidebar
  []
  (doto (s/tree)
    (.setRootVisible false)
    (.setShowsRootHandles true)
    (.setModel (DefaultTreeModel. (root-node)))
    (.addTreeSelectionListener
      (reify TreeSelectionListener
        (valueChanged [this e]
          (update-content! (some-> e
                                   .getPath
                                   .getLastPathComponent
                                   .getUserObject)))))
    (-> .getSelectionModel
        (.setSelectionMode TreeSelectionModel/SINGLE_TREE_SELECTION))))

(defn create-content
  []
  (s/editor-pane :id :docs-content
                 :editable? false
                 :content-type "text/html"))

(defn search!
  [& _])

(def ^:dynamic *widgets* [])

(defn create-widgets
  []
  {:search (doto (s/text :id :search
                         :columns 16
                         :listen [:key-released search!])
             (nc-utils/set-accessible-name! :search)
             (editors/text-prompt! (nc-utils/get-string :search)))})

(defn create-card
  []
  (let [widgets (create-widgets)
        widget-bar (ui/wrap-panel :items (map #(get widgets % %) *widgets*))]
    (s/border-panel :id :docs
                    :north widget-bar
                    :west (s/scrollable (create-sidebar) :size [200 :by 0])
                    :center (s/scrollable (create-content)))))

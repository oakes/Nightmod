(ns nightmod.docs
  (:require [nightcode.editors :as editors]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [seesaw.core :as s])
  (:import [javax.swing ListModel]))

(def ns-list ['clojure.core
              'nightmod.public
              'play-clj.core
              'play-clj.g2d
              'play-clj.g2d-physics
              'play-clj.g3d
              'play-clj.g3d-physics
              'play-clj.math
              'play-clj.ui])

(defn should-remove?
  [n]
  (or (.startsWith n "*")
      (.endsWith n "*")))

(def ns-vars (->> ns-list
                  (map #(keys (ns-publics %)))
                  flatten
                  (map str)
                  (remove should-remove?)
                  sort))

(defn create-sidebar
  []
  (s/scrollable (s/listbox :model (reify ListModel
                                    (addListDataListener [this l])
                                    (getElementAt [this i]
                                      (nth ns-vars i))
                                    (getSize [this]
                                      (count ns-vars))
                                    (removeListDataListener [this l])))
                :size [200 :by 0]))

(defn create-content
  []
  (s/scrollable (s/text "hi")))

(defn search!
  [& _])

(def ^:dynamic *widgets* [:search])

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
                    :west (create-sidebar)
                    :center (create-content))))

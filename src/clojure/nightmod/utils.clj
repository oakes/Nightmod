(ns nightmod.utils
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [nightcode.dialogs :as dialogs]
            [nightcode.editors :as editors]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [seesaw.core :as s])
  (:import [java.awt BorderLayout KeyboardFocusManager]
           [java.text SimpleDateFormat]))

(def ^:const window-width 1200)
(def ^:const window-height 768)
(def ^:const editor-width 700)
(def ^:const core-file "core.clj")
(def ^:const settings-file "settings.edn")
(def ^:const docs-name "*Docs*")
(def ^:const repl-name "*REPL*")
(def ^:const game-ns 'nightmod.run)

(def main-dir (atom nil))
(def project-dir (atom nil))
(def error (atom nil))
(def out (atom ""))
(def stack-trace? (atom false))
(def editor (atom nil))

(defn get-data-dir
  []
  (.getCanonicalPath (io/file (System/getProperty "user.home") "Nightmod")))

(defn format-date
  [unix-time]
  (.format (SimpleDateFormat. "yyyy.MM.dd HH:mm:ss") unix-time))

(defn format-project-dir
  [project-name]
  (-> project-name
      (string/replace " " "-")
      nc-utils/format-project-name))

(defn new-project-name!
  [template]
  (dialogs/show-text-field-dialog!
    (nc-utils/get-string :enter-project-name)
    (nc-utils/get-string template)))

(defn new-project-dir!
  [project-name]
  (let [dir-name (format-project-dir project-name)
        project-file (io/file @main-dir dir-name)]
    (cond
      (= 0 (count dir-name))
      (dialogs/show-simple-dialog! (nc-utils/get-string :invalid-name))
      (.exists project-file)
      (dialogs/show-simple-dialog! (nc-utils/get-string :file-exists))
      :else
      project-file)))

(defn new-project!
  [template project-name project-file]
  (.mkdirs project-file)
  (doseq [file-name (-> (str template "/_files.edn")
                        io/resource
                        slurp
                        edn/read-string)]
    (-> (str template "/" file-name)
        io/resource
        io/input-stream
        (io/copy (io/file project-file file-name))))
  (->> (format (slurp (io/resource settings-file)) project-name)
       (spit (io/file project-file settings-file)))
  (.getCanonicalPath project-file))

(defn canvas-focus?
  []
  (-> (System/getProperty "os.name") string/lower-case (.indexOf "win") (>= 0)))

(defn add!
  [frame component]
  (if (= :ext (s/id-of frame))
    (.setContentPane frame component)
    (.add (.getContentPane frame) component BorderLayout/EAST)))

(defn remove!
  [frame component]
  (when-not (= :ext (s/id-of frame))
    (.remove (.getContentPane frame) component)))

(defn visibility!
  [frame component show?]
  (.setVisible component (or show? (= :ext (s/id-of frame)))))

(defn toggle-editor!
  ([]
    (toggle-editor! (not (.isVisible @editor))))
  ([show?]
    (if show?
      (add! @ui/root @editor)
      (remove! @ui/root @editor))
    (visibility! @ui/root @editor show?)
    (.revalidate @ui/root)
    ; focus on the root so the game can receive keyboard events
    (when-not show?
      (s/request-focus! @ui/root))))

(defn set-out!
  [s]
  (when (seq s)
    (reset! out s)))

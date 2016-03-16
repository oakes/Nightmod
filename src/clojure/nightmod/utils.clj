(ns nightmod.utils
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [nightcode.dialogs :as dialogs]
            [nightcode.editors :as editors]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [nightmod.input :as input]
            [seesaw.core :as s])
  (:import [java.awt BorderLayout KeyboardFocusManager]
           [java.text SimpleDateFormat]
           [javax.swing JDialog]))

(def ^:const window-width 1200)
(def ^:const window-height 768)
(def ^:const editor-width 700)
(def ^:const core-file "core.clj")
(def ^:const settings-file "settings.edn")
(def ^:const screenshot-file "screenshot.png")
(def ^:const prefs-file ".prefs.edn")
(def ^:const docs-name "*Docs*")
(def ^:const repl-name "*REPL*")
(def ^:const timeout 5000)
(def ^:const game-ns 'nightmod.run)

(def main-dir (atom nil))
(def project-dir (atom nil))
(def error (atom nil))
(def out (atom nil))
(def stack-trace? (atom false))
(def editor (atom nil))
(def last-frame (atom 0))

(defn get-data-dir
  []
  (.getCanonicalPath (io/file (System/getProperty "user.home") "Nightmod")))

(defn format-project-dir
  [project-name]
  (-> project-name
      (string/replace " " "-")
      nc-utils/format-project-name))

(defn dialog-visible?
  []
  (-> (KeyboardFocusManager/getCurrentKeyboardFocusManager)
      .getFocusedWindow
      type
      (isa? JDialog)))

(defn show-simple-dialog!
  [s]
  (when-not (dialog-visible?)
    (dialogs/show-simple-dialog! s)))

(defn new-project-name!
  [default-name]
  (when-not (dialog-visible?)
    (dialogs/show-text-field-dialog!
      (nc-utils/get-string :enter-project-name)
      (nc-utils/get-string default-name))))

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
  (doseq [file-name (-> (str template "/files.edn")
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
   (if show?
     ; clear the key down buffer so keys don't get stuck in the down position
     (input/clear-key-buffer!)
     ; focus on the root so the game can receive keyboard events
     (s/request-focus! @ui/root))))

(defn set-out!
  [s initialize?]
  (when (or initialize? (seq s))
    (reset! out s)))

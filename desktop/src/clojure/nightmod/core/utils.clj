(ns nightmod.core.utils
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.xml :as xml])
  (:import [java.io File]
           [java.nio.file Files Path]
           [java.util Locale]))

; preferences

(defn write-pref!
  "Writes a key-value pair to the preference file."
  [k v])

(defn read-pref
  "Reads value from the given key in the preference file."
  [k])

; language

(def lang-files {"en" "values/strings.xml"})
(def lang-strings (-> (get lang-files (.getLanguage (Locale/getDefault)))
                      (or (get lang-files "en"))
                      io/resource
                      .toString
                      xml/parse
                      :content))

(defn get-string
  "Returns the localized string for the given keyword."
  [res-name]
  (if (keyword? res-name)
    (-> (filter #(= (get-in % [:attrs :name]) (name res-name))
                lang-strings)
        first
        :content
        first
        (or "")
        (clojure.string/replace "\\" ""))
    res-name))

; paths and encodings

(defn get-version
  "Gets the version number from the project.clj if possible."
  []
  (let [project-clj (->> (io/resource "project.clj")
                         slurp
                         read-string
                         (binding [*read-eval* false]))]
    (if (= (name (nth project-clj 1)) "nightcode")
      (nth project-clj 2)
      "beta")))

(defn is-parent-path?
  "Determines if the given parent path is equal to or a parent of the child."
  [^String parent-path ^String child-path]
  (or (= parent-path child-path)
      (and parent-path
           child-path
           (.isDirectory (io/file parent-path))
           (.startsWith child-path (str parent-path File/separator)))))

(defn is-text-file?
  "Returns true if the file is of type text, false otherwise."
  [^File file]
  (-> (Files/probeContentType ^Path (.toPath file))
      (or "")
      (.startsWith "text")))

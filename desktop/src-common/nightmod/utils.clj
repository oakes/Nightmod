(ns nightmod.utils
  (:require [clojure.java.io :as io])
  (:import [java.text SimpleDateFormat]))

(def ^:const projects-dir "projects")
(def ^:const properties-file ".properties")

(def main-dir (atom nil))
(def project-dir (atom nil))

(defn get-data-dir
  []
  (let [home-dir (System/getProperty "user.home")
        app-name "Nightmod"
        app-name-lower (clojure.string/lower-case app-name)
        osx-dir (io/file home-dir "Library" "Application Support" app-name)
        win-dir (io/file home-dir "AppData" "Roaming" app-name)]
    (.getCanonicalPath
      (cond
        (.exists (.getParentFile osx-dir)) osx-dir
        (.exists (.getParentFile win-dir)) win-dir
        :else (if-let [config-dir (System/getenv "XDG_CONFIG_HOME")]
                (io/file config-dir app-name-lower)
                (io/file home-dir ".config" app-name-lower))))))

(defn format-date
  [unix-time]
  (.format (SimpleDateFormat. "yyyy.MM.dd HH:mm:ss") unix-time))

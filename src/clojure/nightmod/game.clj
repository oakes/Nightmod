(ns nightmod.game
  (:require [clojail.core :as jail]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [nightcode.utils :as nc-utils]
            [nightmod.manager :as manager]
            [nightmod.sandbox :as sandbox]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.utils :refer :all]))

(defn set-game-screen!
  "Displays one or more screens that you defined with `defscreen`. They will be
run the order they are passed in. Note that only the first screen should call
`(clear!)` in its :on-render function; if the others do, they will clear
whatever was drawn by the preceding screens.

    (set-game-screen! main-screen text-screen)"
  [& game-screens]
  (on-gl (try
           ; clean up
           (asset-manager! manager/manager :clear)
           (stop-timers!)
           ; make sure the game screens are valid
           (doseq [screen game-screens
                   required-key [:show :render :hide :pause :resize :resume]]
             (when (or (not (map? screen))
                       (not (contains? screen required-key)))
               (throw (Exception. "Invalid screen given to set-game-screen!"))))
           ; set the supplied screen(s) with the overlay screen added at the end
           (apply set-screen!
                  screens/nightmod
                  (conj (vec game-screens) screens/overlay-screen))
           ; display error
           (catch Exception e
             (when-not @u/error
               (reset! u/error
                       {:message (nc-utils/get-string :error-load)
                        :exception e}))
             (screens/set-blank-screen!)))))

(defn restart-game!
  "Causes the core.clj file to be run again."
  []
  (on-gl (screens/restart!)))

(defmacro load-game-file
  "Loads a file into the namespace.

    (load-game-file \"utils.clj\")"
  [n]
  (sandbox/safe-read (io/file @u/project-dir n)))

(defn game-pref
  "Retrieves preferences.

    (game-pref)"
  []
  (try (edn/read-string (slurp (io/file @u/project-dir u/prefs-file)))
    (catch Exception _ {})))

(defn game-pref!
  "Stores preferences.

    (game-pref! {:level 10 :health 5}) ; replace all preferences
    (game-pref! :level 10 :health 5) ; add to existing preferences"
  [& keyvals]
  (->> (if (map? (first keyvals))
         (first keyvals)
         (apply assoc (game-pref) keyvals))
       pr-str
       (spit (io/file @u/project-dir u/prefs-file))))

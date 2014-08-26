(ns nightmod.game
  (:require [clojail.core :as jail]
            [clojure.java.io :as io]
            [nightcode.utils :as nc-utils]
            [nightmod.manager :as manager]
            [nightmod.screens :as screens]
            [nightmod.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.utils :refer :all])
  (:import [java.util.concurrent TimeoutException]))

(defn set-game-screen!
  "Displays one or more screens that you defined with `defscreen`. They will be
run the order they are passed in. Note that only the first screen should call
`(clear!)` in its :on-render function; if the others do, they will clear
whatever was drawn by the preceding screens.

    (set-game-screen! main-screen text-screen)"
  [& game-screens]
  (on-gl (try
           ; clean up
           (doseq [gs game-screens]
             (-> gs :screen (swap! #(dissoc % :renderer :layers))))
           (asset-manager! manager/manager :clear)
           (stop-timers!)
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
  (some->> (io/file @u/project-dir n)
           slurp
           (format "(do %s\n)")
           jail/safe-read))

(defn ^:private check-for-timeout!
  "Throws exception if the time since the last frame has exceeded the timeout."
  []
  (when (> (- (System/currentTimeMillis) @u/last-frame) u/timeout)
    (throw (TimeoutException. "Execution timed out."))))

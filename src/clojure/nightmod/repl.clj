(ns nightmod.repl
  (:require [nightcode.editors :as editors]
            [nightcode.ui :as ui]
            [nightcode.utils :as nc-utils]
            [nightmod.utils :as u]
            [seesaw.core :as s]))

(defn redirect-io
  [[in out] func]
  (binding [*out* out
            *err* out
            *in* in]
    (func)))

(defn start-thread!*
  [in-out func]
  (->> (fn []
         (try (func)
           (catch Exception e (some-> (.getMessage e) println))
           (finally (println "\n===" (nc-utils/get-string :finished) "==="))))
       (redirect-io in-out)
       (fn [])
       Thread.
       .start))

(defmacro start-thread!
  [in-out & body]
  `(start-thread!* ~in-out (fn [] ~@body)))

(defn run-repl!
  [in-out]
  (start-thread! in-out (clojure.main/repl :init #(in-ns u/game-ns)
                                           :print clojure.pprint/pprint)))

(def ^:dynamic *widgets* [:restart])

(defn create-widgets
  [actions]
  {:restart (ui/button :id :restart
                       :text (nc-utils/get-string :restart)
                       :listen [:action (:restart actions)])})

(defn create-card
  []
  (let [console (editors/create-console u/repl-name)
        run! (fn [& _]
               (run-repl! (ui/get-io! console))
               (s/request-focus! (.getTextArea console)))
        actions {:restart run!}
        widgets (create-widgets actions)
        widget-bar (ui/wrap-panel :items (map #(get widgets % %) *widgets*))]
    (doto (.getTextArea console)
      (s/config! :id :repl-console)
      (nc-utils/set-accessible-name! :repl-console))
    (run!)
    (s/border-panel :north widget-bar :center console)))

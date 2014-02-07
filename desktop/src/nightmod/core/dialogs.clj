(ns nightmod.core.dialogs
  (:require [clojure.java.io :as io]
            [nightmod.core.utils :as utils]
            [seesaw.core :as s]))

(defn show-shut-down-dialog!
  []
  (-> (s/dialog :content (utils/get-string :quit_confirm)
                :options [(s/button :text (utils/get-string :quit)
                                    :listen [:action
                                             #(s/return-from-dialog % true)])
                          (s/button :text (utils/get-string :cancel)
                                    :listen [:action
                                             #(s/return-from-dialog % false)])])
      s/pack!
      s/show!))

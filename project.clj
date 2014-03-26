(defproject nightmod "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  
  :dependencies [[clojail "1.0.6"]
                 [com.badlogicgames.gdx/gdx "0.9.9"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "0.9.9"]
                 [com.badlogicgames.gdx/gdx-platform "0.9.9"
                  :classifier "natives-desktop"]
                 [com.cemerick/pomegranate "0.3.0"]
                 [nightcode "0.3.2"
                  :exclusions [leiningen
                               lein-ancient
                               lein-cljsbuild
                               lein-droid
                               lein-fruit
                               play-clj/lein-template]]
                 [org.clojure/clojure "1.6.0"]
                 [org.eclipse.jgit "3.2.0.201312181205-r"]
                 [play-clj "0.2.3"]
                 [seesaw "1.4.4"]]
  
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :aot [nightmod.core]
  :main nightmod.core)

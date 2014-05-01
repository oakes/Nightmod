(defproject nightmod "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  
  :dependencies [[com.badlogicgames.gdx/gdx "1.0.0"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.0.0"]
                 [com.badlogicgames.gdx/gdx-box2d "1.0.0"]
                 [com.badlogicgames.gdx/gdx-box2d-platform "1.0.0"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-bullet "1.0.0"]
                 [com.badlogicgames.gdx/gdx-bullet-platform "1.0.0"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-platform "1.0.0"
                  :classifier "natives-desktop"]
                 [com.cemerick/pomegranate "0.3.0"]
                 [nightcode "0.3.4-SNAPSHOT"
                  :exclusions [leiningen
                               lein-ancient
                               lein-cljsbuild
                               lein-droid
                               lein-fruit
                               play-clj/lein-template]]
                 [org.clojars.oakes/clojail "1.0.6"]
                 [org.clojure/clojure "1.6.0"]
                 [org.eclipse.jgit "3.2.0.201312181205-r"]
                 [play-clj "0.3.2-SNAPSHOT"]
                 [seesaw "1.4.4"]]
  
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :aot [nightmod.core]
  :main nightmod.core)

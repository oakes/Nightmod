(defproject nightmod "0.2.1"
  :description "A tool for making live-moddable games in Clojure"
  :url "https://github.com/oakes/Nightmod"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :dependencies [[clojail "1.0.6"]
                 [com.badlogicgames.gdx/gdx "1.4.1"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.4.1"]
                 [com.badlogicgames.gdx/gdx-box2d "1.4.1"]
                 [com.badlogicgames.gdx/gdx-box2d-platform "1.4.1"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-bullet "1.4.1"]
                 [com.badlogicgames.gdx/gdx-bullet-platform "1.4.1"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-platform "1.4.1"
                  :classifier "natives-desktop"]
                 [nightcode "0.4.1"
                  :exclusions [gwt-plugin
                               leiningen
                               lein-ancient
                               lein-cljsbuild
                               lein-clr
                               lein-droid
                               lein-fruit
                               lein-typed
                               play-clj/lein-template]]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/core.logic "0.8.3"]
                 [org.clojure/tools.reader "0.8.9"]
                 [play-clj "0.4.1"]
                 [play-clj.net "0.1.2"]
                 [pldb "0.1.5"]
                 [seesaw "1.4.4"]]
  :resource-paths ["resources"]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :aot [nightmod.core]
  :main ^:skip-aot nightmod.Nightmod
  :manifest {"SplashScreen-Image" "logo_splash.png"})

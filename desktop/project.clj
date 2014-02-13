(defproject nightmod "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  
  :dependencies [[com.badlogicgames.gdx/gdx "0.9.9"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "0.9.9"]
                 [com.badlogicgames.gdx/gdx-platform "0.9.9"
                  :classifier "natives-desktop"]
                 [com.github.insubstantial/substance "7.2.1"]
                 [com.fifesoft/autocomplete "2.5.0"]
                 [com.fifesoft/rsyntaxtextarea "2.5.0"]
                 [compliment "0.0.3"]
                 [org.clojure/clojure "1.5.1"]
                 [org.flatland/ordered "1.5.2"]
                 [org.lpetit/paredit.clj "0.19.3"]
                 [net.java.balloontip/balloontip "1.2.4.1"]
                 [play-clj "0.2.0"]
                 [seesaw "1.4.4"]]
  :repositories [["sonatype"
                  "https://oss.sonatype.org/content/repositories/snapshots/"]]
  
  :source-paths ["src/clojure" "src-common"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :aot [nightmod.core.desktop-launcher]
  :main nightmod.core.desktop-launcher)

(defproject chess-game "0.1.0-SNAPSHOT"
  :description "Chess engine and AI bot in Clojure"
  :url "https://github.com/mark-watson/Clojure-AI-Book"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :main ^:skip-aot chess-game.cli
  :aliases {"perft" ["run" "-m" "chess-game.perft"]}
  :profiles {:uberjar {:aot :all}})

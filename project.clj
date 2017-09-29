(defproject animation "1.0"
  :dependencies [
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.562"]
                 [cljsjs/d3 "4.3.0-5"]
                 [cljsjs/jquery "3.2.1-0"]
                 [cljsjs/semantic-ui "2.2.4-0"]
                 ]
  :plugins [[lein-figwheel "0.5.4"]]
  :clean-targets [:target-path "out"]
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [figwheel-sidecar "0.5.4"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel {:websocket-host "localhost"
                                   :on-jsload "animation.core/main"}
                        :compiler {:main "animation.core"
                                   :asset-path "js/out"
                                   :output-to "resources/public/js/main.js"
                                   :output-dir "resources/public/js/out"}}]}

  :figwheel {:nrepl-middleware ["cider.nrepl/cider-middleware"
                                "refactor-nrepl.middleware/wrap-refactor"
                                "cemerick.piggieback/wrap-cljs-repl"]})

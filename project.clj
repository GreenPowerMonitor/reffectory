(defproject greenpowermonitor/reffectory "0.1.0-SNAPSHOT"
  :description ""
  ;:url "https://github.com/GreenPowerMonitor/reffectory"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  ;:scm {:name "git"
  ;      :url "https://github.com/GreenPowerMonitor/reffectory"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]]

  :profiles {:dev {:dependencies [[greenpowermonitor/test-doubles "0.1.3-SNAPSHOT"]
                                  [org.clojure/core.async "0.4.474"]]}}

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.10"]
            [lein-auto "0.1.3"]
            [lein-cljfmt "0.6.0"]]

  :doo {:build "unit-tests"
        :alias {:default [:node]}}

  :auto {"test" {:file-pattern #"\.(clj|cljs|cljc|edn)$"}}

  :clean-targets ^{:protect false} ["resources/public/js" "target" "out"]

  :cljsbuild {:builds [{:id           "unit-tests"
                        :source-paths ["src" "test"]
                        :compiler     {:output-to     "out/unit_tests.js"
                                       :main          greenpowermonitor.unit-tests-runner
                                       :target        :nodejs
                                       :optimizations :none}}]})

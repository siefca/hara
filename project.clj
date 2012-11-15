(defproject hara "0.6.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :aot [hara.data.ova]
  ;;:java-source-paths ["src/java"]
  :source-paths ["src/clojure"]
  :profiles {:dev {:dependencies [[midje "1.4.0"]]}})

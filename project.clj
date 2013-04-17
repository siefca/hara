(defproject hara "0.7.0"
  :description "Utilities library of Common Functions"
  :url "http://github.com/zcaudate/hara"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :aot [hara.ova.impl 
        hara.propagator.cell
        hara.propagator.propagator]
  :profiles {:dev {:dependencies [[midje "1.5.0"]
                                  [clj-time "0.4.4"]]}})

(defproject im.chit/hara "1.0.3"
  :description "General purpose utilities library "
  :url "http://github.com/zcaudate/hara"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :codox {:exclude [hara.common]}
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [clj-time "0.5.1"]]}})

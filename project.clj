(defproject im.chit/hara "2.0.2"
  :description "General purpose utilities library "
  :url "http://github.com/zcaudate/hara"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [clj-time "0.6.0"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-repack "0.1.2"]]}}
                           
  :repack {:exclude [core]
           :levels 2})

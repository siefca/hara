(defproject im.chit/hara "2.1.1"
  :description "General purpose utilities library "
  :url "https://github.com/zcaudate/hara"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-midje-doc "0.0.23"]
                             [lein-repack "0.1.4"]]}}
  :documentation {:files {"docs/index"
                        {:input "test/midje_doc/hara/outline.clj"
                         :title "hara"
                         :sub-title "General Purpose Utilities Library"
                         :author "Chris Zheng"
                         :email  "z@caudate.me"}}}
  :repack {:levels 2})

(ns hara.io.watch-test
  (:use midje.sweet)
  (:require [hara.io.watch :refer :all]
            [hara.common.watch :as watch]
            [clojure.java.io :as io]))

^{:refer hara.io.watch/watcher :added "2.1"}
(fact "playing with the watch service"
  (def ^:dynamic *happy* (promise))

  (watch/add (io/file ".") :save
             (fn [_ _ _ [cmd file]]
               (deliver *happy* [cmd (.getName file)]))
             {:filter [".hara"]
              :exclude [".git" "target"]
              :async true})

  (watch/list (io/file "."))
  => (contains {:save fn?})

  (spit "happy.hara" "hello")

  @*happy*
  => [:create "happy.hara"]

  (.delete (io/file "happy.hara"))
  (watch/remove (io/file ".") :save))

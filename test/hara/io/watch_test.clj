(ns hara.io.watch-test
  (:use midje.sweet)
  (:require [hara.io.watch :refer :all]
            [hara.common.watch :as watch]
            [clojure.java.io :as io]))

^{:refer hara.io.watch/watcher :added "2.1"}
(fact "the watch interface provided for java.io.File"

  (def ^:dynamic *happy* (promise))

  (watch/add (io/file ".") :save
             (fn [f k _ [cmd file]]
               (watch/remove f k)
               (.delete file)
               (deliver *happy* [cmd (.getName file)]))
             {:types #{:create :modify}
              :recursive false
              :filter  [".hara"]
              :exclude [".git" "target"]
              :async false})

  (watch/list (io/file "."))
  => (contains {:save fn?})

  (spit "happy.hara" "hello")

  @*happy*
  => [:create "happy.hara"]

  (watch/list (io/file "."))
  => {})

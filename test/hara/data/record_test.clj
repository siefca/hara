(ns hara.data.record-test
  (:use midje.sweet)
  (:require [hara.data.record :refer :all]))

^{:refer hara.data.record/empty-record :added "2.1"}
(fact "creates an empty record from an existing one"

  (defrecord Database [host port])
  (empty-record (Database. "localhost" 8080))
  => (just {:host nil :port nil}))

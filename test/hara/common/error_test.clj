(ns hara.common.error-test
  (:use midje.sweet)
  (:require [hara.common.error :refer :all]))

^{:refer hara.common.error/error :added "2.0"}
(fact "Throws an exception when called."

  (error "This is an error")
  => (throws Exception "This is an error")

  (error (Exception. "This is an error")
         "This is a chained error")
  => (throws Exception "This is a chained error"))

^{:refer hara.common.error/suppress :added "2.0"}
(fact "Suppresses any errors thrown in the body."

  (suppress (error "Error")) => nil

  (suppress (error "Error") :error) => :error

  (suppress (error "Error")
            (fn [e]
              (.getMessage e))) => "Error")

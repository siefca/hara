(ns hara.reflect.core.query-instance-test
  (:use midje.sweet)
  (:require [hara.reflect.core.query-instance :refer :all])
  (:refer-clojure :exclude [.*]))

^{:refer hara.reflect.core.query-instance/.* :added "2.1"}
(fact "lists what methods could be applied to a particular instance"

  (.* "abc" :name #"^to")
  => ["toCharArray" "toLowerCase" "toString" "toUpperCase"]

  (.* String :name #"^to")
  => ["toString"])

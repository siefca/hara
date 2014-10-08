(ns hara.reflect.common-test
  (:use midje.sweet)
  (:require [hara.reflect.common :refer :all]))

^{:refer hara.reflect.common/context-class :added "2.1"}
(fact "If x is a class, return x otherwise return the class of x"

  (context-class String)
  => String

  (context-class "")
  => String)

^{:refer hara.reflect.common/combinations :added "2.1"}
(fact "find all combinations of `k` in a given input list `l`"

  (combinations 2 [1 2 3])
  => [[2 1] [3 1] [3 2]]

  (combinations 3 [1 2 3 4])
  => [[3 2 1] [4 2 1] [4 3 1] [4 3 2]])

^{:refer hara.reflect.common/all-subsets :added "2.1"}
(fact "finds all non-empty sets of collection `s`"

  (all-subsets [1 2 3])
  => [#{1} #{2} #{3} #{1 2} #{1 3} #{2 3} #{1 2 3}])

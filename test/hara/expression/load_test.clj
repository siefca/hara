(ns hara.expression.load-test
  (:use midje.sweet)
  (:require [hara.expression.load :refer :all])
  (:refer-clojure :exclude [load]))

^{:refer hara.expression.load/load-single :added "2.1"}
(fact "Perform a single step in the load process"
  ^:hidden
  (load-single {:a 1} [:b '(inc (:a %))]) => {:a 1 :b 2})

^{:refer hara.expression.load/load :added "2.1"}
(fact "Seeds an initial map using forms"

  (load {:a 1} [:b '(inc (:a %))
                :c '(+ (:a %) (:b %))]) 
  => {:a 1 :b 2 :c 3})
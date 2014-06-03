(ns hara.function.args-test
  (:use midje.sweet)
  (:require [hara.function.args :refer :all]))

^{:refer hara.function.args/vargs? :added "2.1"}
(fact "checks that function contain variable arguments"

  (vargs? (fn [x])) => false

  (vargs? (fn [x & xs])) => true)

^{:refer hara.function.args/varg-count :added "2.1"}
(fact "counts the number of arguments types before variable arguments"

  (varg-count (fn [x y & xs])) => 2
  
  (varg-count (fn [x])) => nil)

^{:refer hara.function.args/arg-count :added "2.1"}
(fact "counts the number of non-varidic argument types"

  (arg-count (fn [x])) => [1]
  
  (arg-count (fn [x & xs])) => []
  
  (arg-count (fn ([x]) ([x y]))) => [1 2])
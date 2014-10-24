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

^{:refer hara.function.args/arg-check :added "2.1"}
(fact "counts the number of non-varidic argument types"

  (arg-check (fn [x]) 1) => true

  (arg-check (fn [x & xs]) 1) => true

  (arg-check (fn [x & xs]) 0)
  => (throws Exception "Function must accomodate 0 arguments"))

^{:refer hara.function.args/op :added "2.1"}
(fact "loose version of apply. Will adjust the arguments to put into a function"

  (op + 1 2 3 4 5 6) => 21

  (op (fn [x] x) 1 2 3) => 1

  (op (fn [_ y] y) 1 2 3) => 2
  
  (op (fn [_] nil)) => (throws Exception))

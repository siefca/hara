(ns hara.reflect.core.apply-test
  (:use midje.sweet)
  (:require [hara.reflect.core.apply :refer :all])
  (:refer-clojure :exclude [.>]))


^{:refer hara.reflect.core.apply/.> :added "2.1"}
(fact "Threads the first input into the rest of the functions. Same as `->` but
   allows access to private fields using both `:keyword` and `.symbol` lookup:"

  (.> "abcd" :value String.) => "abcd"

  (.> "abcd" .value String.) => "abcd"

  (let [a  "hello"
        _  (.> a (.value (char-array "world")))]
    a)
  => "world")

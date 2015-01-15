(ns vinyasa.reflection-test
  (:use midje.sweet)
  (:require [vinyasa.reflection :refer :all])
  (:refer-clojure :exclude [.% .%> .? .* .& .> .>ns .>var]))

^{:refer vinyasa.reflection/.> :added "2.1"}
(fact "Threads the first input into the rest of the functions. Same as `->` but
   allows access to private fields using both `:keyword` and `.symbol` lookup:"

  (.> "abcd" :value String.) => "abcd"

  (.> "abcd" .value String.) => "abcd"

  (let [a  "hello"
        _  (.> a (.value (char-array "world")))]
    a)
  => "world")
(ns hara.common.hash-test
  (:use midje.sweet)
  (:require [hara.common.hash :refer :all]))

^{:refer hara.common.hash/hash-label :added "2.0"}
(fact "Returns a keyword repesentation of the hash-code. For use in 
   generating internally unique keys"

   (hash-label 1) => "__1__"
   (hash-label "a" "b" "c") => "__97_98_99__"
   (hash-label "abc") => "__96354__")
(ns hara.reflect.core.delegate-test
  (:use midje.sweet)
  (:require [hara.reflect.core.delegate :refer :all]))

^{:refer hara.reflect.core.delegate/delegate :added "2.1"}
(fact "Allow transparent field access and manipulation to the underlying object."

  (let [a   "hello"
        >a  (delegate a)]

    (seq (>a :value)) => [\h \e \l \l \o]

    (>a :value (char-array "world"))
    a => "world"))

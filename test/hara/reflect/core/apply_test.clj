(ns hara.reflect.core.apply-test
  (:use midje.sweet)
  (:require [hara.reflect.core.apply :refer :all]))

^{:refer hara.reflect.core.apply/apply-element :added "2.1"}
(fact "apply the class element to arguments"

  (seq (apply-element "123" "value" []))
  => [\1 \2 \3])

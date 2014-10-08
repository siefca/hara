(ns hara.reflect.types.modifiers-test
  (:use midje.sweet)
  (:require [hara.reflect.types.modifiers :refer :all]))

^{:refer hara.reflect.types.modifiers/int-to-modifiers :added "2.1"}
(fact "converts the modifier integer into human readable represenation"

  (int-to-modifiers 2r001100)
  => #{:protected :static}

  (int-to-modifiers 128 :field)
  => #{:transient}

  (int-to-modifiers 128 :method)
  => #{:varargs})

^{:refer hara.reflect.types.modifiers/modifiers-to-int :added "2.1"}
(fact "converts the human readable represenation of modifiers into an int"

  (modifiers-to-int #{:protected :static})
  => 12

  (modifiers-to-int #{:transient :field})
  => 128)

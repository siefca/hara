(ns hara.class-test
  (:use midje.sweet)
  (:require [hara.class :refer :all :as class]))

(def prn-class nil)

^{:refer hara.class/defclassmulti :added "2.1"}
(fact "Defines class-based multimethod dispatch. Supporting methods are
  very similar to defmulti, defmethod, and remove-method

    - defclassmethod
    - remove-classmethod"

  (defclassmulti  prn-class [cls])
  (defclassmethod prn-class CharSequence [cls] "Chars")
  (defclassmethod prn-class Number [cls] "Number")
  (defclassmethod prn-class Float  [cls] "Float")

  (prn-class Float)  => "Float"
  (prn-class Long)   => "Number"
  (prn-class String) => "Chars"
  (prn-class (type {})) => (throws Exception))

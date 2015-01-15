(ns hara.reflect.core.query-test
  (:use midje.sweet)
  (:require [hara.reflect.core.query :refer :all]))

^{:refer hara.reflect.core.query/query-class :added "2.1"}
(fact "queries the java view of the class declaration"

  (query-class String  [#"^c" :name])
  => ["charAt" "checkBounds" "codePointAt" "codePointBefore"
      "codePointCount" "compareTo" "compareToIgnoreCase"
      "concat" "contains" "contentEquals" "copyValueOf"])

^{:refer hara.reflect.core.query/query-instance :added "2.1"}
(fact "lists what methods could be applied to a particular instance"

  (query-instance "abc" [:name #"^to"])
  => ["toCharArray" "toLowerCase" "toString" "toUpperCase"]

  (query-instance String [:name #"^to"])
  => (contains ["toString"]))

(ns hara.reflect.core.query-class-test
  (:use midje.sweet)
  (:require [hara.reflect.core.query-class :refer :all])
  (:refer-clojure :exclude [.?]))

^{:refer hara.reflect.core.query-class/.? :added "2.1"}
(fact "queries the java view of the class declaration"

  (.? String  #"^c" :name)
  => ["charAt" "checkBounds" "codePointAt" "codePointBefore"
      "codePointCount" "compareTo" "compareToIgnoreCase"
      "concat" "contains" "contentEquals" "copyValueOf"])

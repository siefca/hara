(ns hara.reflect.core.hierarchy-test
  (:use midje.sweet)
  (:require [hara.reflect.core.hierarchy :refer :all])
  (:refer-clojure :exclude [.% .%>]))

^{:refer hara.reflect.core.hierarchy/.% :added "2.1"}
(fact "Lists class information"

  (.% String)  ;; or (.%> "")
  => (contains {:name "java.lang.String"
                :tag :class
                :hash anything
                :container nil
                :modifiers #{:instance :class :public :final}
                :static false
                :delegate java.lang.String}))

^{:refer hara.reflect.core.hierarchy/.%> :added "2.1"}
(fact "Lists the class and interface hierarchy for the class"

  (.%> String)   ;; or (.%> "")
  => [java.lang.String
      [java.lang.Object
       #{java.io.Serializable
         java.lang.Comparable
         java.lang.CharSequence}]])

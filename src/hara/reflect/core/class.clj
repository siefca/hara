(ns hara.reflect.core.class
  (:require [hara.reflect.common :as common]
            [hara.reflect.element.common :as element]
            [hara.class.inheritance :as inheritance]))

(defn class-info
  "Lists class information

  (class-info String)
  => (contains {:name \"java.lang.String\"
                :tag :class
                :hash anything
                :container nil
                :modifiers #{:instance :class :public :final}
                :static false
                :delegate java.lang.String})"
  {:added "2.1"}
  [obj]
  (element/seed :class (common/context-class obj)))

(defn class-hierarchy
  "Lists the class and interface hierarchy for the class

  (class-hierarchy String)
  => [java.lang.String
      [java.lang.Object
       #{java.io.Serializable
         java.lang.Comparable
         java.lang.CharSequence}]]"
  {:added "2.1"}
  [obj]
  (let [t (common/context-class obj)]
    (vec (cons t (inheritance/ancestor-tree t)))))

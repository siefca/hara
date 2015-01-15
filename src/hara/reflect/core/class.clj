(ns hara.reflect.core.class
  (:require [hara.reflect.common :as common]
            [hara.reflect.element.common :as element]
            [hara.class.inheritance :as inheritance]))

(defn class-info [obj]
  (element/seed :class (common/context-class obj)))

(defn class-hierarchy [obj]
  (let [t (common/context-class obj)]
    (vec (cons t (inheritance/ancestor-tree t)))))

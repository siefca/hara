(ns hara.reflect.core.query-instance
  (:require [hara.class.inheritance :as inheritance]
            [hara.reflect.core.query-class :as q]
            [hara.reflect.pretty.classes :as classes]
            [hara.reflect.pretty.args :as args]
            [hara.reflect.pretty.display :as display])
  (:refer-clojure :exclude [.*]))

(defn all-instance-elements
  [tcls icls]
  (let [supers (reverse (inheritance/ancestor-list tcls))
        eles   (mapcat #(q/list-class-elements % [:instance]) supers)]
    (concat eles
            (if icls (concat eles (q/list-class-elements icls [:static]))))))

(defn list-instance-elements
  [obj selectors]
  (let [grp (args/args-group selectors)
        tcls (type obj)]
    (->> (all-instance-elements tcls (if (class? obj) obj))
         (display/display grp))))

(defmacro .*
  "lists what methods could be applied to a particular instance

  (.* \"abc\" :name #\"^to\")
  => [\"toCharArray\" \"toLowerCase\" \"toString\" \"toUpperCase\"]

  (.* String :name #\"^to\")
  => [\"toString\"]"
  {:added "2.1"}
  [obj & selectors]
  `(list-instance-elements ~obj ~(args/args-convert selectors)))

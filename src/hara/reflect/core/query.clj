(ns hara.reflect.core.query
  (:require [hara.reflect.common :as common]
            [hara.reflect.types.element :as types]
            [hara.reflect.element multi method field constructor]
            [hara.class.inheritance :as inheritance]
            [hara.reflect.pretty.args :as args]
            [hara.reflect.pretty.display :as display]))

(defn- all-class-members
  [^Class class]
  (concat
     (seq (.getDeclaredMethods class))
     (seq (.getDeclaredConstructors class))
     (seq (.getDeclaredFields class))))

(defn list-class-elements
  ([class]
     (->> (all-class-members class)
          (map types/to-element)))
  ([class selectors]
     (let [grp (args/args-group selectors)]
       (->> (list-class-elements class)
            (display/display grp)))))

(defn query-class
  "queries the java view of the class declaration

  (query-class String  [#\"^c\" :name])
  => [\"charAt\" \"checkBounds\" \"codePointAt\" \"codePointBefore\"
      \"codePointCount\" \"compareTo\" \"compareToIgnoreCase\"
      \"concat\" \"contains\" \"contentEquals\" \"copyValueOf\"]"
  {:added "2.1"}
  [obj selectors]
  (list-class-elements (common/context-class obj) selectors))


(defn all-instance-elements
  [tcls icls]
  (let [supers (reverse (inheritance/ancestor-list tcls))
        eles   (mapcat #(list-class-elements % [:instance]) supers)]
    (concat eles
            (if icls (concat eles (list-class-elements icls [:static]))))))

(defn query-instance
  "lists what methods could be applied to a particular instance

  (query-instance \"abc\" [:name #\"^to\"])
  => [\"toCharArray\" \"toLowerCase\" \"toString\" \"toUpperCase\"]

  (query-instance String [:name #\"^to\"])
  => (contains [\"toString\"])"
  {:added "2.1"}
  [obj selectors]
  (let [grp (args/args-group selectors)
        tcls (type obj)]
    (->> (all-instance-elements tcls (if (class? obj) obj))
         (display/display grp))))


(comment

  (query-class String [:name #"join"])

  (defmacro .?
  "queries the java view of the class declaration

  (.? String  #\"^c\" :name)
  => [\"charAt\" \"checkBounds\" \"codePointAt\" \"codePointBefore\"
  \"codePointCount\" \"compareTo\" \"compareToIgnoreCase\"
  \"concat\" \"contains\" \"contentEquals\" \"copyValueOf\"]"
  {:added "2.1"}
  [obj & selectors]
  `(list-class-elements (common/context-class ~obj) ~(args/args-convert selectors))))

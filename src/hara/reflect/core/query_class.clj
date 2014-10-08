(ns hara.reflect.core.query-class
  (:require [hara.reflect.common :as common]
            [hara.reflect.types.element :as types]
            [hara.reflect.element multi method field constructor]
            [hara.reflect.pretty.args :as args]
            [hara.reflect.pretty.display :as display])
  (:refer-clojure :exclude [.?]))

(defn- all-class-members
  [class]
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

(defmacro .?
  "queries the java view of the class declaration

  (.? String  #\"^c\" :name)
  => [\"charAt\" \"checkBounds\" \"codePointAt\" \"codePointBefore\"
      \"codePointCount\" \"compareTo\" \"compareToIgnoreCase\"
      \"concat\" \"contains\" \"contentEquals\" \"copyValueOf\"]"
  {:added "2.1"}
  [obj & selectors]
  `(list-class-elements (common/context-class ~obj) ~(args/args-convert selectors)))

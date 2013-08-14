(ns hara.common.control
  (:refer-clojure :exclude [if-let]))

(defmacro if-let
 "An alternative to if-let where more bindings can be added"
  ([bindings then]
    `(if-let ~bindings ~then nil))
  ([bindings then else]
    (let [[op rhs & more] (seq bindings)]
      `(clojure.core/if-let [~op ~rhs]
         ~(if more
            `(if-let [~@more] ~then ~else)
            then)
         ~else))))

(defmacro case-let
 "An alternative to if-let where more bindings can be added"
  [[bnd expr] & more]
    `(let [~bnd ~expr]
       (case ~bnd ~@more)))

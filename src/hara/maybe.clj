(ns hara.maybe
  (refer-clojure :exclude [comp if-let when-let]))

(defn comp [& fs]
  (fn comp-fn
    ([i] (comp-fn i fs))
    ([i [f & more]]
       (cond (nil? i) nil
             (nil? f) i
             :else (recur (f i) more)))))

(defmacro if-let
  ([bindings then]
    `(hara.maybe/if-let ~bindings ~then nil))
  ([[bnd expr & more] then else]
    `(clojure.core/if-let [~bnd ~expr]
        ~(if more
            `(hara.maybe/if-let [~@more] ~then ~else)
            then)
        ~else)))

(defmacro when-let
  [[bnd expr & more] & body]
  `(clojure.core/when-let [~bnd ~expr]
     ~@(if more
          [`(hara.maybe/when-let [~@more]
             ~@body)]
          body)))

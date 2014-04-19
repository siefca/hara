(ns hara.core
  (:refer-clojure :exclude [comp if-let when-let]))

(defn comp
  "Same as `clojure.core/comp` except that the functions will shortcircuit on nil.

  ((comp inc inc) 1) => 3
  ((comp inc inc) nil) => nil"
  {:added "2.0"}
  [& fs]
  (fn comp-fn
    ([i] (comp-fn i fs))
    ([i [f & more]]
       (cond (nil? i) nil
             (nil? f) i
             :else (recur (f i) more)))))

(defmacro if-let
  "Same as `clojure.core/if-let` except that more than one var can be binded.
  If any of the bounded vars are `nil`, then the expression will short circuit to the other branch

  (if-let [a 1 b 2] (+ a b)) => 3

  (if-let [a nil b 2] (+ a b) :nil) => :nil"
  {:added "2.0"}
  ([bindings then]
    `(hara.core/if-let ~bindings ~then nil))
  ([[bnd expr & more] then else]
    `(clojure.core/if-let [~bnd ~expr]
        ~(if more
            `(hara.core/if-let [~@more] ~then ~else)
            then)
        ~else)))

(defmacro when-let
  "Same as `clojure.core/when-let` except that more than one var can be binded.
  If any of the bounded vars are `nil`, then the expression will short circuit to return nil

  (when-let [a 1 b 2] (+ a b)) => 3

  (when-let [a nil b 2] (+ a b)) => nil"
  {:added "2.0"}
  [[bnd expr & more] & body]
  `(clojure.core/when-let [~bnd ~expr]
     ~@(if more
          [`(hara.core/when-let [~@more]
             ~@body)]
          body)))

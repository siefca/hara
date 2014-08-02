(ns hara.extend.all
  (:require [clojure.walk :as walk]))

(defn extend-single
  "Transforms a protocol template into an extend-type expression

  (extend-single 'Type
                 'IProtocol
                 '[(op [x y] (% x y))]
                 '[op-object])
  => '(clojure.core/extend-type Type IProtocol (op [x y] (op-object x y)))"
  {:added "2.1"}
  [t proto ptmpls funcs]
  (apply list `extend-type t proto
    (map (fn [tmpl f] (walk/prewalk-replace {'% f} tmpl))
       ptmpls funcs)))

(defn extend-entry [proto ptmpls [ts funcs]]
  (cond (vector? ts)
        (map #(extend-single % proto ptmpls funcs) ts)

        :else
        [(extend-single ts proto ptmpls funcs)]))

(defmacro extend-all
  "Transforms a protocl template into multiple extend-type expresions

  (macroexpand-1
   '(extend-all Magma
                [(op ([x y] (% x y)) )]

                Number        [op-number]
                [List Vector] [op-list]))
  => '(do (clojure.core/extend-type Number Magma (op ([x y] (op-number x y))))
          (clojure.core/extend-type List Magma (op ([x y] (op-list x y))))
          (clojure.core/extend-type Vector Magma (op ([x y] (op-list x y)))))"
  {:added "2.1"}
  [proto ptmpls & args]
  (let [types (partition 2 args)]
    `(do
       ~@(mapcat #(extend-entry proto ptmpls %) types))))
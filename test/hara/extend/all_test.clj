(ns hara.extend.all-test
  (:use midje.sweet)
  (:require [hara.extend.all :refer :all]))

^{:refer hara.extend.all/extend-single :added "2.1"}
(fact "Transforms a protocol template into an extend-type expression"

  (extend-single 'Type
                 'IProtocol
                 '[(op [x y] (% x y))]
                 '[op-object])
  => '(clojure.core/extend-type Type IProtocol (op [x y] (op-object x y))))


^{:refer hara.extend.all/extend-all :added "2.1"}
(fact "Transforms a protocl template into multiple extend-type expresions"

  (macroexpand-1
   '(extend-all Magma
                [(op ([x y] (% x y)) )]

                Number        [op-number]
                [List Vector] [op-list]))
  => '(do (clojure.core/extend-type Number Magma (op ([x y] (op-number x y))))
          (clojure.core/extend-type List Magma (op ([x y] (op-list x y))))
          (clojure.core/extend-type Vector Magma (op ([x y] (op-list x y))))))
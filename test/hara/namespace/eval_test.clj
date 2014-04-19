(ns hara.namespace.eval-test
  (:use midje.sweet)
  (:require [hara.namespace.eval :refer :all]))

^{:refer hara.namespace.eval/with-ns :added "2.0"}
(fact "Evaluates `body` forms in an existing namespace given by `ns`."

  (require '[hara.common.checks])
  (with-ns 'hara.common.checks
    (long? 1)) => true)

^{:refer hara.namespace.eval/with-temp-ns :added "2.0"}
(fact "Evaluates `body` forms in a temporaryily created namespace."

  (with-temp-ns
    (def  inc1 inc)
    (defn inc2 [x] (+ 1 x))
    (-> 1 inc1 inc2))
  => 3

  "All created vars will be destroyed after evaluation."

  (resolve 'inc1) => nil)

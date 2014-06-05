(ns hara.common.map-test
  (:use midje.sweet)
  (:require [hara.common.map :as map]))

^{:refer hara.common.map/get}
(fact "get")

^{:refer hara.common.map/get-in}
(fact "get-in")

^{:refer hara.common.map/to}
(fact "to")

(comment
  ^{:refer hara.common.map/classify}
  (fact "classify")

  ^{:refer hara.common.map/coerce}
  (fact "coerce")

  ^{:refer hara.common.map/from}
  (fact "from"))
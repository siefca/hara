(ns hara.protocol.state-test
  (:use midje.sweet)
  (:require [hara.protocol.state :refer :all]
            [hara.state])
  (:refer-clojure :exclude [get set]))

^{:refer hara.protocol.state/get}
(fact "get")

^{:refer hara.protocol.state/set}
(fact "set")

^{:refer hara.protocol.state/update}
(fact "update")
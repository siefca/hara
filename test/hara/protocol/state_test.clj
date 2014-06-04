(ns hara.protocol.state-test
  (:use midje.sweet)
  (:require [hara.protocol.state :refer :all :as p]
            [hara.state])
  (:refer-clojure :exclude [get set]))

^{:refer hara.protocol.state/get :added "2.1"}
(fact "Like deref but is extensible through the IStateful protocol"

  (p/get (atom 1)) => 1
  
  (p/get (ref 1)) => 1)

^{:refer hara.protocol.state/set :added "2.1"}
(fact "Like reset! but is extensible through the IStateful protocol"

  (let [a (atom nil)]
    (p/set a 1)
    @a) => 1)

^{:refer hara.protocol.state/update :added "2.1"}
(fact "Like swap! but is extensible through the IStateful protocol"

  (let [a (atom 0)]
    (p/update a inc)
    @a) => 1)
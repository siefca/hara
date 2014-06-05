(ns hara.common.state-test
  (:use midje.sweet)
  (:require [hara.common.state :as state]
            [hara.common.checks :refer :all])
  (:refer-clojure :exclude [get set]))

^{:refer hara.common.state/get :added "2.1"}
(fact "Like deref but is extensible through the IStateGet protocol"

  (state/get (atom 1)) => 1

  (state/get (ref 1)) => 1)

^{:refer hara.common.state/set :added "2.1"}
(fact "Like reset! but is extensible through the IStateSet protocol"

  (let [a (atom nil)]
    (state/set a 1)
    @a) => 1)

^{:refer hara.common.state/update :added "2.1"}
(fact "Like swap! but is extensible through the IStateSet protocol"

  (let [a (atom 0)]
    (state/update a + 1)
    @a) => 1

  ^:hidden
  (let [a (atom 0)]
    (state/update a inc)
    @a) => 1)

^{:refer hara.common.state/dispatch :added "2.1"}
(fact "Updates the value contained within a container using another thread."

  (let [res (state/dispatch (atom 0)
                (fn [x]  (inc x)))]
    res   => future?
    @res  => atom?
    @@res => 1))

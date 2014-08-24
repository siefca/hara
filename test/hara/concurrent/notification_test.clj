(ns hara.concurrent.notification-test
  (:use midje.sweet)
  (:require [hara.concurrent.notification :refer :all]
            [hara.common.state :as state]
            [hara.common.checks :refer :all]))

^{:refer hara.concurrent.notification/dispatch :added "2.1"}
(fact "Updates the value contained within a ref or atom using another thread."
  @@(dispatch (atom 0)
              (fn [x]
                (Thread/sleep 200)
                (inc x)))
  => 1)

^{:refer hara.concurrent.notification/notify :added "2.1"}
(fact "Creates a watch mechanism so that when a long running function
  finishes, it returns a promise that delivers the updated iref."
  (let [res (notify #(do (Thread/sleep 200)
                         (state/update % inc))
                    (ref 1))]
    res   => promise?
    @res  => iref?
    @@res => 2))

^{:refer hara.concurrent.notification/wait-on :added "2.1"}
(fact  "Waits for a long running multithreaded function to update the
  ref. Used for testing purposes"

  (let [atm (atom 0)
        f (fn [obj] (dispatch obj #(do (Thread/sleep 300)
                                      (inc %))))]
    (wait-on f atm)
    @atm => 1))

^{:refer hara.concurrent.notification/alter-on :added "2.1"}
(fact  "A redundant function. Used for testing purposes. The same as
   `(alter! ref f & args)` but the function is wired with the
   notification scheme."
  (let [atm (atom 0)
        _   (alter-on atm #(do (Thread/sleep 300)
                               (inc %)))]
    @atm => 1))

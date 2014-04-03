(ns hara.test-state
  (:use midje.sweet)
  (:require [hara.state :as h]
            [hara.type-check :refer [promise? atom?]]))

(fact "hash-keyword"
  (h/hash-keyword 1) => :__1__
  (h/hash-keyword 1 "id") => :__id_1__
  (h/hash-keyword 1 "1") => :__1_1__
  (h/hash-keyword "hello") => :__99162322__)


(fact "hash-code"
  (h/hash-code 1) => 1
  (h/hash-code :1) => 1013907437
  (h/hash-code "1") => 49)

(fact "hash-keyword"
  (h/hash-keyword 1) => :__1__
  (h/hash-keyword :1) => :__1013907437__)

(fact "hash-pair"
  (h/hash-pair 1 :1) => :__1_1013907437__)

(fact "set-value!"
  @(h/set-value! (atom 0) 1) => 1
  @(h/set-value! (ref 0) 1) => 1)

(fact "alter!"
  @(h/alter! (atom 0) inc) => 1
  @(h/alter! (ref 0) inc) => 1)

(defn slow-inc
  ([v] (slow-inc v 50))
  ([v ms]
     (Thread/sleep ms)
     (inc v)))

(fact "dispatch!"
  (h/dispatch! (atom 0) slow-inc) => promise?
  (let [in (atom 0)]
    (h/wait-on slow-inc in)
    @in => 1
    (h/wait-on slow-inc in)
    @in => 2))

(fact "make-change-watch"
  (let [wf (h/make-change-watch :a vector)]
    (wf :key :ref {:a 1} {:a 2}) => [:key :ref 1 2]
    (wf :key :ref {:a 1} {:a 1}) => nil
    (wf :key :ref {:a nil} {:a 1}) => [:key :ref nil 1]
    (wf :key :ref {:a 1} {:a nil}) => nil))

(fact "add-change-watch"
  (let [a (atom {:a 1 :b 2})
        b (atom nil)
        _ (h/add-change-watch a :clone :b (fn [& _] (reset! b @a)))]
    (swap! a assoc :a 0)
    @b => nil

    (swap! a assoc :b 1)
    @b => {:a 0 :b 1}

    (swap! a assoc :a 1)
    @b => {:a 0 :b 1}))

(fact "latch"
  (let [in  (atom 0)
        out (atom 0)]
    (h/latch in out)
    (reset! in 10)
    @out => 10

    (h/delatch in out)
    (reset! in 0)
    @out => 10))

(fact "latch-on-change"
  (let [in  (atom {:a 0 :b 0})
        out (atom nil)]
    (h/latch-changes in out :b)
    (swap! in assoc :a 1)
    @out => nil
    (swap! in assoc :b 1)
    @out => 1

    (h/latch-changes in out :b #(* % 2))
    (swap! in assoc :b 2)
    @out => 4
    (swap! in assoc :b nil)
    @out => 4
    (swap! in assoc :b 8)
    @out => 16
    (swap! in update-in [:b] inc)
    @in => {:a 1 :b 9}
    @out => 18

    (h/delatch in out)
    (swap! in assoc :b 10)
    @out => 18))

(fact "run-notify"
  (let [res (h/run-notify
             #(do (Thread/sleep 200)
                  (h/alter! % inc)) (atom 1) h/notify-on-all)]
    res => promise?
    @res => atom?
    @@res => 2)


  (let [res (h/run-notify
             #(do (Thread/sleep 200)
                  (h/alter! % update-in [:a] inc))
             (atom {:a 1}) (h/notify-on-change :a))]
    res => promise?
    @res => atom?
    @@res => {:a 2}))

(fact "wait-deref"
  (h/wait-deref (atom 1)) => 1
  (h/wait-deref (promise) 10) => nil
  (h/wait-deref (promise) 10 :NA) => :NA)


(fact "wait-for"
  (let [atm (atom 1)
        f   #(h/dispatch! % slow-inc)
        ret (f atm)]
    @atm => 1
    ret => (complement future-done?))

  (let [atm (atom 1)
        f   #(h/dispatch! % slow-inc)
        ret (h/wait-for f atm)]
    @atm => 2
    ret => atom?
    @ret => 2))

(fact "wait-on"
  (let [atm (h/wait-on slow-inc (atom 1))]
    @atm => 2) )

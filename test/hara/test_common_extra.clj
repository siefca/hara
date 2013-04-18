(ns hara.test-common-extra
  (:use midje.sweet)
  (:require [hara.common :as h]))

(defn within-interval [min max]
  (fn [x]
    (and (> x min) (< x max))))

(defn approx
  [val error] (within-interval (- val error) (+ val error)))

(fact "time-ms"
  (h/time-ms (inc 1)) => (approx 0 0.01)
  (h/time-ms (Thread/sleep 100)) => (approx 100 2))

(fact "atom?"
  (h/atom? (atom 0)) => true)

(fact "aref?"
  (h/aref? (ref 0)) => true)

(fact "iref?"
  (h/iref? (atom 0)) => true
  (h/iref? (ref 0)) => true)

(fact "ideref?"
  (h/ideref? (atom 0)) => true
  (h/ideref? (ref 0)) => true
  (h/ideref? (promise)) => true)

(fact "set-value!"
  @(h/set-value! (atom 0) 1) => 1
  @(h/set-value! (ref 0) 1) => 1)

(fact "alter!"
  @(h/alter! (atom 0) inc) => 1
  @(h/alter! (ref 0) inc) => 1)

(fact "make-change-watch"
  (let [wf (h/make-change-watch :a vector)]
    (wf :key :ref {:a 1} {:a 2}) => [:key :ref 1 2]
    (wf :key :ref {:a 1} {:a 1}) => nil
    (wf :key :ref {:a nil} {:a 1}) => [:key :ref nil 1]
    (wf :key :ref {:a 1} {:a nil}) => nil))

(fact "latch"
  (let [in  (atom 0)
        out (atom 0)]
    (h/latch in out)
    (reset! in 10)
    @out => 10

    (h/unlatch in out)
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

    (h/unlatch in out)
    (swap! in assoc :b 10)
    @out => 16))

(defn slow-inc
  ([v] (slow-inc v 500))
  ([v ms]
     (Thread/sleep ms)
     (inc v)))

(fact "dispatch!"
  (let [in (atom 1)]
    (h/dispatch! in slow-inc)))






(comment

(h/wait-on slow-inc (atom 10) 1000)

 (defn slow-assoc [m k v]
   (Thread/sleep 500)
   (assoc m k v))

 (def mpin (atom {:a 1 :b 2 :c 3}))

 (wait-till-notified #(dispatch! % slow-assoc :b 4)
                     mpin
                     (done-on-sel :b) 1000 :NA)

 (wait-till-notified #(dispatch! % slow-assoc :a 10)
                     mpin)

 (defn slow-inc-mt [rf]
   (dispatch! rf inc))

 (def ain (atom 1))

 (etime (alter! ain slow-inc))
 (etime (dispatch! ain slow-inc))

 (inc-mt ain)
 ()
 (wait-for-notification #(dispatch! % slow-inc) ain)
 (demand-change #(dispatch! % inc) ain)
 (demand-change #(dispatch! % identity) ain identity 500 :NA)

 (expect #(dispatch! % slow-inc) ain)

 ()

 (defmacro wait )


 (wait [a (expecting rf1 mtf1)
        b (rf1 mtf2)])

 (defn defer! rf f )

 (defmacro wait)
)

#_(facts "watch-for-change produces another function that can be used in watching
        the nested values of maps contained in atoms and iotams.

          @usage
          I have an atom containing a large array of nested keys:

             (def a (atom {:k0
                           {:k1
                            ....
                            {:kn value}}})

          And I only wish to run <fn> when the value changes, then this function can be used
          to generate a function for watch.

              (add-watch a (watch-for-change [:k0 :k1 ... :kn] <fn>))
       "
  (let [itm      (atom {:a {:b {:c {:d nil}}}})
        out1     (atom nil)
        to-out1-fn (fn [k r p v] (reset! out1 v))]
    (add-watch itm  :test (f/watch-for-change [:a :b :c :d] to-out1-fn))
    (fact "assert that adding watch does not change any of the
           values of the atoms: the nested value within itm and out1 are nil"
      @itm => {:a {:b {:c {:d nil}}}}
      @out1 => nil)

    (reset! itm {:a {:b {:c {:d 1}}}})
    (fact "assert that itm is updated and out1 has also been manipulated through the watch"
      @itm => {:a {:b {:c {:d 1}}}}
      @out1 => 1)

    (swap! itm update-in [:a :b :c :d] inc)
    (fact "assert that itm is updated  and out has been manipulated"
      @itm => {:a {:b {:c {:d 2}}}}
      @out1 => 2)))

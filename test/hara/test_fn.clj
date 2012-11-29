(ns hara.test-fn
  (:use midje.sweet)
  (:require [hara.fn :as f]
            [clj-time.core :as t] :reload))

(facts "call-if-not-nil only executes fn if the input to fn is not nil

          @usage (call-if-not-nil fn input)
       "
  (f/call-if-not-nil inc nil) => (throws Exception)
  (f/call-if-not-nil inc 1) => 2
  (f/call-if-not-nil + 1 1) => 2
  (f/call-if-not-nil + 1 1 1) => 3
  (f/call-if-not-nil + 1 1 1 1) => 4
  (f/call-if-not-nil + 1 1 1 1 1) => 5
  (f/call-if-not-nil + 1 1 1 1 1 1) => 6)

(facts "look-up returns the nested value of a map according to nested keys :k0 to :kn
        if the value is not present, then the function returns nil

          @usage (look-up map [:k0 :k1 ... :kn])
       "
  (f/look-up nil nil) => nil
  (f/look-up nil []) => nil
  (f/look-up {} []) => {}
  (f/look-up {} [:any]) => nil
  (f/look-up {:a 1} [:a]) => 1
  (f/look-up {:a {:b 1}} [:a]) => {:b 1}
  (f/look-up {:a {:b 1}} [:a :b]) => 1
  (f/look-up {:a {:b 1}} [:a :c]) => nil
  (f/look-up {:a {:b {:c 1}}} [:a :b :c]) => 1
  (f/look-up {:a {:b {:c 1}}} [:b :c]) => nil
  (f/look-up [0 1 2 3 4 5] nil) => [0 1 2 3 4 5]
  (f/look-up [0 1 2 3 4 5] []) => [0 1 2 3 4 5]
  (f/look-up [0 1 2 3 4 5] [2]) => 2
  (f/look-up [{1 [0 0 {3 4}]}] [0 1 2 3]) => 4
  (f/look-up {:a [{:b 0} {:b 1}]} [:a 0 :b]) => 0
  (f/look-up {:a [{:b 0} {:b 1}]} [:a 1 :b]) => 1)

(declare ops)
(facts "msg uses a map as a 'object'
          @usage (msg obj :action arg1 arg2
       "
  (against-background (ops) => {:add + :sub -})
  (f/msg (ops) :add) => 0
  (f/msg (ops) :add 1) => 1
  (f/msg (ops) :add 1 1) => 2
  (f/msg (ops) :add 1 1 1) => 3
  (f/msg (ops) :add 1 1 1 1) => 4
  (f/msg (ops) :sub 3) => -3
  (f/msg (ops) :sub 3 1) => 2
  (f/msg (ops) :sub 3 1 1) => 1
  (f/msg (ops) :sub 3 1 1 1) => 0)

(facts "watch-for-change produces another function that can be used in watching
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


(facts "manipulate* is a higher order function that acts on
        elements nested in clojure arrays and data-structures

         @usage: (manipulate* fn data-structure)
        "
  ;; Basic operations
  (f/manipulate* nil nil)           => (throws Exception)
  (f/manipulate* identity nil)      => nil
  (f/manipulate* identity 1)        => 1
  ;;(f/manipulate* identity (int-array 1 2))    => '(1 2)
  (f/manipulate* identity [1 2])    => [1 2]
  (f/manipulate* identity {:a :b})  => {:a :b}
  (f/manipulate* identity #{:a :b}) => #{:a :b}
  ;; Atoms
  (deref (f/manipulate* identity (atom [1 2])))    => [1 2]
  ;; Functions
  (f/manipulate* #(* % 2) nil)      => (throws Exception)
  (f/manipulate* #(* % 2) 1)        => 2
  (f/manipulate* #(* % 2) [1 2])    =>  [2 4]
  (f/manipulate* #(* % 2) #{1 2})   => #{2 4}
  (f/manipulate* vector [1 {2 3}]) => [[1] {[2] [3]}]
  (f/manipulate* #(* % 2) {1 [2 3] #{4 5} 6 7 '(8 (9 (10)))}) => {2 [4 6] #{8 10} 12 14 '(16 (18 (20)))})

(fact "A specialised function can be used for custom manipulation"
  (f/manipulate* (fn [x] (* 2
                           (cond (string? x) (Integer/parseInt x)
                                 :else x)))
                 {1 "2" 3 ["4" 5 #{6 "7"}]})
  => {2 4 6 [8 10 #{12 14}]})

(fact "Customized type functions can be used for deconstruction and construction"
  (f/manipulate* (fn [x] (* 2 x))
                 {1 "2" 3 ["4" 5 #{6 "7"}]}
                 [{:pred String
                   :dtor (fn [x] (Integer/parseInt x))}])
  => {2 4 6 [8 10 #{12 14}]}

  (f/manipulate* (fn [x] (* 2 x))
                 {1 "2" 3 ["4" 5 #{6 "7"}]}
                 [{:pred String
                   :dtor (fn [x] (Integer/parseInt x))
                   :ctor (fn [x] (.toString x))}])
  => {2 "4" 6 ["8" 10 #{12 "14"}]}

  (f/manipulate* (fn [x] (* 2 x))
                 {1 "2" 3 ["4" 5 #{6 "7"}]}
                 [{:pred String
                   :dtor (fn [x] (Integer/parseInt x))
                   :ctor (fn [x] [(.toString x)])}])
  => {2 ["4"] 6 [["8"] 10 #{12 ["14"]}]}

  (f/manipulate* (fn [x] (* 2 x))
                 {1 "2" 3 ["4" 5 #{6 "7"}]}
                 [{:pred String
                   :dtor (fn [x] [(Integer/parseInt x)])
                   :ctor (fn [x] (.toString x))}])
  => {2 "[4]" 6 ["[8]" 10 #{12 "[14]"}]})

(fact "Different types of containers"
  (f/manipulate* #(* 2 %)
                 (java.util.Vector. [1 2 3])
                 [{:pred java.util.Vector
                   :dtor seq}])
  => '(2 4 6)

  (f/manipulate* #(* 2 %)
                 (java.util.Vector. [1 2 3])
                 [{:pred java.util.Vector
                   :dtor seq
                   :ctor (fn [x] (apply hash-set x))}])
  => #{2 4 6})

(fact "Predictates on numbers"
  (f/manipulate* identity
                 [1 2 3 4 5]
                 [{:pred #(= 2 %)
                   :dtor (fn [x] 10)}])
  => [1 10 3 4 5])

(fact "Predictates on vectors"
  (f/manipulate* identity
                 [1 [:date 2 3 4 5] 6 7]
                 [{:pred #(and (vector? %) (= (first %) :date))
                   :dtor #(apply t/date-time (rest %))}])
  => [1 (t/date-time 2 3 4 5) 6 7])

(fact "Predictates on vectors"
  (f/manipulate* identity
                 [1 (t/date-time 2 3 4 5) 6 7]
                 [{:pred org.joda.time.DateTime
                   :dtor (fn [dt] [:date (t/year dt) (t/month dt)])}])
  => [1 [:date 2 3] 6 7])

(fact "Predictates on numbers"
  (f/manipulate* identity
                 [1 2 3 4 5]
                 [{:pred #(= 2 %)
                   :ctor (fn [x] 10)}])
  => (throws StackOverflowError))




(facts "deref* dereferences nested elements

         @usage: (deref* fn data-structure)"
  (f/deref* nil) => nil
  (f/deref* 1) =>  1
  (f/deref* (atom 1)) => 1
  (f/deref* (atom (atom (atom 1)))) => 1
  (f/deref* (atom {:a (atom {:b (atom :c)})})) => {:a {:b :c}}
  (f/deref* {(atom 1) (atom 2)}) => {1 2}

  ;; advanced
  (f/deref* #(* 2 %) (atom 1)) => 2
  @(f/deref* #(atom (* 2 %)) (atom 1)) => 2 ;; stupid but plausible
  (f/deref* #(* 2 %)
            (atom (atom (atom "1")))
            [{:pred String
              :dtor (fn [x] (Integer/parseInt x))
              :ctor (fn [x] (.toString x))}]) => "2")

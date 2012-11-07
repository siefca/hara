(ns hara.test-iotam
  (:use midje.sweet
        hara.iotam)
  (:require [hara.fn :as f]))

(facts "call-if-not-nil only executes fn if the input to fn is not nil

          @usage (call-if-not-nil fn input)
       "
  (f/call-if-not-nil inc nil) => nil
  (f/call-if-not-nil inc 1) => 2)

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
  (f/look-up {:a {:b {:c 1}}} [:b :c]) => nil)

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

(facts "manipulate* advanced uses"

  (fact "A specialised function can be used for custom manipulation"
    (f/manipulate* (fn [x] (* 2
                             (cond (string? x) (Integer/parseInt x)
                                   :else x)))
                   {1 "2" 3 ["4" 5 #{6 "7"}]})
    => {2 4 6 [8 10 #{12 14}]})

  (fact "Customized type functions can be used for deconstruction and construction"
    (f/manipulate* (fn [x] (* 2 x))
                       {1 "2" 3 ["4" 5 #{6 "7"}]}
                       [{:type String
                         :dtor (fn [x] (Integer/parseInt x))
                         :ctor identity}])
    => {2 4 6 [8 10 #{12 14}]}

    (f/manipulate* (fn [x] (* 2 x))
                   {1 "2" 3 ["4" 5 #{6 "7"}]}
                   [{:type String
                     :dtor (fn [x] (Integer/parseInt x))
                     :ctor (fn [x] (.toString x))}])
    => {2 "4" 6 ["8" 10 #{12 "14"}]})

  (fact "Different types of containers"
    (f/manipulate* #(* 2 %)
                   (java.util.Vector. [1 2 3])
                   [{:type java.util.Vector
                     :dtor seq
                     :ctor identity}])
    => '(2 4 6)

    (f/manipulate* #(* 2 %)
                   (java.util.Vector. [1 2 3])
                   [{:type java.util.Vector
                     :dtor seq
                     :ctor (fn [x] (apply hash-set x))}])
    => #{2 4 6}))


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
            [{:type String
              :dtor (fn [x] (Integer/parseInt x))
              :ctor (fn [x] (.toString x))}]) => "2")

(ns hara.test-utils
  (:use midje.sweet
        [hara.utils :only [bytes?]])
  (:require [hara.utils :as h]))

(fact "type-checker"
  (h/type-checker :string) => (exactly #'clojure.core/string?)
  (h/type-checker :bytes) =>  (exactly #'hara.utils/bytes?)
  (h/type-checker :other) =>  nil)

(fact "func-map creates a hashmap using as key the function applied to each
       element of the collection."
  (h/func-map identity [1 2 3]) => {1 1 2 2 3 3}
  (h/func-map #(* 2 %) [1 2 3]) => {2 1 4 2 6 3}
  (h/func-map #(* 2 %) [1 1 1]) => {2 1}

  (h/func-map :id [{:id 1 :val 1} {:id 2 :val 2}])
  => {1 {:id 1 :val 1} 2 {:id 2 :val 2}}

  "Same :ids will cause the first to be overwritten"
  (h/func-map :id [{:id 1 :val 1} {:id 1 :val 2}])
  => {1 {:id 1 :val 2}})

(fact "remove-repeats outputs a filtered list of values"
  (h/remove-repeats [1 1 2 2 3 3 4 5 6]) => [1 2 3 4 5 6]
  (h/remove-repeats :n [{:n 1} {:n 1} {:n 1} {:n 2} {:n 2}]) => [{:n 1} {:n 2}]
  (h/remove-repeats even? [2 4 6 1 3 5]) => [2 1])

(fact "dissoc-in"
  (h/dissoc-in {:a 2 :b 2} [:a]) => {:b 2}
  (h/dissoc-in {:a 2 :b 2} [:a] true) => {:b 2}

  (h/dissoc-in {:a {:b 2 :c 3}} [:a :b]) => {:a {:c 3}}
  (h/dissoc-in {:a {:b 2 :c 3}} [:a :b] true) => {:a {:c 3}}

  (h/dissoc-in {:a {:c 3}} [:a :c]) => {}
  (h/dissoc-in {:a {:c 3}} [:a :c] true) => {:a {}}

  (h/dissoc-in {:a {:b {:c 3}}} [:a :b :c]) => {}
  (h/dissoc-in {:a {:b {:c 3}}} [:a :b :c] true) => {:a {:b {}}})

(fact "assocm"
  (h/assocm {} :b 1) => {:b 1}
  (h/assocm {:a 1} :b 1) => {:a 1 :b 1}
  (h/assocm {:a 1} :a 1) => {:a 1}
  (h/assocm {:a 1} :a 2) => {:a #{1 2}}
  (h/assocm {:a #{1}} :a 2) => {:a #{1 2}}
  (h/assocm {:a 1} :a #{2}) => {:a #{1 2}})

(fact "dissocm"
  (h/dissocm {:a 1} :a) => {}
  (h/dissocm {:a 1} [:a #{2}]) => {:a 1}
  (h/dissocm {:a 1} [:a #{1}]) => {}
  (h/dissocm {:a 1} [:a #{1 2}]) => {}
  (h/dissocm {:a #{1 2}} [:a 1]) => {:a #{2}}
  (h/dissocm {:a #{1 2}} [:a 0]) => {:a #{1 2}}
  (h/dissocm {:a #{1 2}} [:a #{1}]) => {:a #{2}}
  (h/dissocm {:a #{1 2}} [:a #{0 1}]) => {:a #{2}}
  (h/dissocm {:a #{1 2}} [:a #{1 2}]) => {})

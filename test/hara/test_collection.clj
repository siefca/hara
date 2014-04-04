(ns hara.test-collection
  (:require [hara.collection :as h]
            [clj-time.core :as t]
            [midje.sweet :refer :all]
            [hara.common.type-check :refer [hash-map? bytes?]]
            [hara.common.error :refer [suppress]]))

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

(fact "replace-walk"
  (h/replace-walk 1 {1 2})
  => 2

  (h/replace-walk [1 2 3] {1 2})
  => [2 2 3]

  (h/replace-walk '[1 (1 2 [1 2 3])] {1 3 3 1})
  => '[3 (3 2 [3 2 1])]

  (h/replace-walk {:a 1 :b {:c 1}} {1 2})
  => {:a 2 :b {:c 2}}

  (h/replace-walk '{:a 1 :b {:c [1 (1 [1 1])]}} {1 2})
  => '{:a 2 :b {:c [2 (2 [2 2])]}})

(fact "group-bys"
  (h/group-bys even? [1 2 3 4 5])
  {false #{1 3 5}, true #{2 4}})

(facts "manipulate is a higher order function that acts on
          elements nested in clojure arrays and data-structures

           @usage: (manipulate fn data-structure)
          "
  ;; Basic operations
  (h/manipulate nil nil)           => (throws Exception)
  (h/manipulate identity nil)      => nil
  (h/manipulate identity 1)        => 1
  ;;(h/manipulate identity (int-array 1 2))    => '(1 2)
  (h/manipulate identity [1 2])    => [1 2]
  (h/manipulate identity {:a :b})  => {:a :b}
  (h/manipulate identity #{:a :b}) => #{:a :b}
  ;; Atoms
  (deref (h/manipulate identity (atom [1 2])))    => [1 2]
  ;; Functions
  (h/manipulate #(* % 2) nil)      => (throws Exception)
  (h/manipulate #(* % 2) 1)        => 2
  (h/manipulate #(* % 2) [1 2])    =>  [2 4]
  (h/manipulate #(* % 2) #{1 2})   => #{2 4}
  (h/manipulate vector [1 {2 3}]) => [[1] {2 [3]}]
  (h/manipulate vector [1 {2 3}]
                [{:pred hash-map?
                  :ctor #(into {} %)
                  :dtor seq}])
  => [[1] {[2] [3]}]
  (h/manipulate #(* % 2) {1 [2 3] #{4 5} 6 7 '(8 (9 (10)))})
  => {1 [4 6], #{4 5} 12, 7 '(16 (18 (20)))})

(fact "A specialised function can be used for custom manipulation"
  (h/manipulate (fn [x] (* 2
                           (cond (string? x) (Integer/parseInt x)
                                 :else x)))
                 {1 "2" 3 ["4" 5 #{6 "7"}]})
  => {1 4 3 [8 10 #{12 14}]})

(fact "Customized type functions can be used for deconstruction and construction"
  (h/manipulate (fn [x] (* 2 x))
                {1 "2" 3 ["4" 5 #{6 "7"}]}
                [{:pred String
                  :dtor (fn [x] (Integer/parseInt x))}])
  => {1 4 3 [8 10 #{12 14}]}

  (h/manipulate (fn [x] (* 2 x))
                {1 "2" 3 ["4" 5 #{6 "7"}]}
                [{:pred String
                  :dtor (fn [x] (Integer/parseInt x))
                  :ctor (fn [x] (.toString x))}])
  => {1 "4" 3 ["8" 10 #{12 "14"}]}

  (h/manipulate (fn [x] (* 2 x))
                {1 "2" 3 ["4" 5 #{6 "7"}]}
                [{:pred String
                  :dtor (fn [x] (Integer/parseInt x))
                  :ctor (fn [x] [(.toString x)])}])
  => {1 ["4"] 3 [["8"] 10 #{12 ["14"]}]}

  (h/manipulate (fn [x] (* 2 x))
                {1 "2" 3 ["4" 5 #{6 "7"}]}
                [{:pred String
                  :dtor (fn [x] [(Integer/parseInt x)])
                  :ctor (fn [x] (.toString x))}])
  => {1 "[4]" 3 ["[8]" 10 #{12 "[14]"}]})

(fact "Different types of containers"
  (h/manipulate #(* 2 %)
                (java.util.Vector. [1 2 3])
                [{:pred java.util.Vector
                  :dtor seq}])
  => '(2 4 6)

  (h/manipulate #(* 2 %)
                (java.util.Vector. [1 2 3])
                [{:pred java.util.Vector
                  :dtor seq
                  :ctor (fn [x] (apply hash-set x))}])
  => #{2 4 6})

(fact "Predictates on numbers"
  (h/manipulate identity
                [1 2 3 4 5]
                [{:pred #(= 2 %)
                  :dtor (fn [x] 10)}])
  => [1 10 3 4 5])

(fact "Predictates on vectors"
  (h/manipulate identity
                [1 [:date 2 3 4 5] 6 7]
                [{:pred #(and (vector? %) (= (first %) :date))
                  :dtor #(apply t/date-time (rest %))}])
  => [1 (t/date-time 2 3 4 5) 6 7])

(fact "Predictates on vectors"
  (h/manipulate identity
                [1 (t/date-time 2 3 4 5) 6 7]
                [{:pred org.joda.time.DateTime
                  :dtor (fn [dt] [:date (t/year dt) (t/month dt)])}])
  => [1 [:date 2 3] 6 7])

(fact "Predictates on numbers"
  (h/manipulate identity
                [1 2 3 4 5]
                [{:pred #(= 2 %)
                  :ctor (fn [x] 10)}])
  => (throws StackOverflowError))


(facts "deref-nested dereferences nested elements

           @usage: (deref-nested fn data-structure)"
  (h/deref-nested nil) => nil
  (h/deref-nested 1) =>  1
  (h/deref-nested (atom 1)) => 1
  (h/deref-nested (atom (atom (atom 1)))) => 1
  (h/deref-nested (atom {:a (atom {:b (atom :c)})})) => {:a {:b :c}}
  (h/deref-nested {:a (atom 2)}) => {:a 2}

  ;; advanced
  (h/deref-nested #(* 2 %) (atom 1)) => 2
  @(h/deref-nested #(atom (* 2 %)) (atom 1)) => 2 ;; stupid but plausible
  (h/deref-nested #(* 2 %)
                  (atom (atom (atom "1")))
                  [{:pred String
                    :dtor (fn [x] (Integer/parseInt x))
                    :ctor (fn [x] (.toString x))}]) => "2")

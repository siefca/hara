(ns hara.test-collection
  (:require [hara :as h]
            [clj-time.core :as t]
            [midje.sweet :refer :all]
            [hara.type-check :refer [bytes?]]))

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

(fact "dissoc-in"
  (h/dissoc-in {:a 2 :b 2} [:a]) => {:b 2}
  (h/dissoc-in {:a 2 :b 2} [:a] true) => {:b 2}

  (h/dissoc-in {:a {:b 2 :c 3}} [:a :b]) => {:a {:c 3}}
  (h/dissoc-in {:a {:b 2 :c 3}} [:a :b] true) => {:a {:c 3}}

  (h/dissoc-in {:a {:c 3}} [:a :c]) => {}
  (h/dissoc-in {:a {:c 3}} [:a :c] true) => {:a {}}

  (h/dissoc-in {:a {:b {:c 3}}} [:a :b :c]) => {}
  (h/dissoc-in {:a {:b {:c 3}}} [:a :b :c] true) => {:a {:b {}}})

(fact "assoc-nil"
  (h/assoc-nil {} :a 1) => {:a 1}
  (h/assoc-nil {:a 1} :a 2) => {:a 1}
  (h/assoc-nil {:a 1} :a 2 :b 2) => {:a 1 :b 2})

(fact "assoc-nil-in"
  (h/assoc-nil-in {} [:a] 1) => {:a 1}
  (h/assoc-nil-in {} [:a :b] 1) => {:a {:b 1}}
  (h/assoc-nil-in {:a {:b 1}} [:a :b] 2) => {:a {:b 1}})

(fact "merge-nil"
  (h/merge-nil {} {:a 1}) => {:a 1}
  (h/merge-nil {:a 1} {:a 3}) => {:a 1}
  (h/merge-nil {:a 1 :b 2} {:a 3 :c 3}) => {:a 1 :b 2 :c 3})

(fact "merge-nil-nested"
  (h/merge-nil-nested {} {:a {:b 1}}) => {:a {:b 1}}
  (h/merge-nil-nested {:a {}} {:a {:b 1}}) => {:a {:b 1}}
  (h/merge-nil-nested {:a {:b 1}} {:a {:b 2}}) => {:a {:b 1}}
  (h/merge-nil-nested {:a 1 :b {:c 2}} {:a 3 :e 4 :b {:c 3 :d 3}})
  => {:a 1 :b {:c 2 :d 3} :e 4})

(fact "keys-nested will output all keys in a map"
  (h/keys-nested {:a {:b 1 :c 2}})
  => #{:a :b :c}
  (h/keys-nested {:a {:b 1 :c {:d 2 :e {:f 3}}}})
  => #{:a :b :c :d :e :f})

(fact "dissoc-nested will dissoc all nested keys in a map"
  (h/dissoc-nested {:a 1} [:a])
  => {}

  (h/dissoc-nested {:a 1 :b 1} [:a :b])
  => {}

  (h/dissoc-nested {:a {:b 1 :c {:b 1}}} [:b])
  => {:a {:c {}}}

  (h/dissoc-nested {:a {:b 1 :c {:b 1}}} [:a :b])
  => {})


(fact "diff-nested will take two maps and compare what in the first is different to that in the second"
  (h/diff-nested {} {}) => {}
  (h/diff-nested {:a 1} {}) => {:a 1}
  (h/diff-nested {:a {:b 1}} {})=> {:a {:b 1}}
  (h/diff-nested {:a {:b 1}} {:a {:b 1}}) => {}
  (h/diff-nested {:a {:b 1}} {:a {:b 1 :c 1}}) => {}
  (h/diff-nested {:a {:b 1 :c 1}} {:a {:b 1}}) => {:a {:c 1}}
  (h/diff-nested {:a 1 :b {:c {:d {:e 1}}}}
                {:a 1 :b {:c {:d {:e 1}}}})
  => {}
  (h/diff-nested {:a 1 :b {:c {:d {:e 1}}}}
                {:a 1 :b {:c 1}})
  => {:b {:c {:d {:e 1}}}})


(fact "merge-nested will take two maps and merge them recursively"
  (h/merge-nested {} {}) => {}
  (h/merge-nested {:a 1} {}) => {:a 1}
  (h/merge-nested {} {:a 1}) => {:a 1}
  (h/merge-nested {:a {:b 1}} {:a {:c 2}}) => {:a {:b 1 :c 2}}
  (h/merge-nested {:a {:b {:c 1}}} {:a {:b {:c 2}}}) => {:a {:b {:c 2}}}
  (h/merge-nested {:a {:b 3}} {:a {:b {:c 3}}}) => {:a {:b {:c 3}}}
  (h/merge-nested {:a {:b {:c 3}}} {:a {:b 3}}) => {:a {:b 3}}
  (h/merge-nested {:a {:b {:c 1 :d 2}}} {:a {:b {:c 3}}}) => {:a {:b {:c 3 :d 2}}})


(fact "remove-nested"
  (h/remove-nested {}) => {}
  (h/remove-nested {:a {}}) => {}
  (h/remove-nested {:a {} :b 1}) => {:b 1}
  (h/remove-nested {:a {:b {:c 1}}}) => {:a {:b {:c 1}}}
  (h/remove-nested {:a {:b {:c {}}}}) => {}
  (h/remove-nested {:a {:b {:c {} :d 1}}}) => {:a {:b {:d 1}}})

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
                [{:pred h/hash-map?
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

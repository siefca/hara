(ns hara.test-common
  (:use midje.sweet
        [hara.common :only [bytes? ?? ?%]])
  (:require [hara.common :as h]
            [clj-time.core :as t]))

(fact "error"
  (h/error "something") => (throws Exception))

(fact "suppress"
  (h/suppress 2) => 2
  (h/suppress (h/error "e")) => nil
  (h/suppress (h/error "e") :error) => :error)

(fact "replace-all"
  (h/replace-all "hello there, hello again" "hello" "bye")
  => "bye there, bye again")

(fact "starts-with"
  (h/starts-with "prefix" "pre") => true
  (h/starts-with "prefix" "suf") => false)

(facts "call "
  (h/call inc nil) => (throws Exception)
  (h/call inc 1) => 2
  (h/call + 1 1) => 2
  (h/call + 1 1 1 1 1 1) => 6
  (h/call nil) => nil
  (h/call nil 1 1 1) => nil)

(fact "call->"
  (h/call-> 4 '(? < 3)) => false
  (h/call-> 4 (list #(< % 3))) => false
  (h/call-> 4 `(< 3)) => false
  (h/call-> 4 '(? < 5)) => true
  (h/call-> 4 (list #(< % 5))) => true
  (h/call-> 4 `(< 5)) => true
  (h/call-> 4 `(+ 1 2 3)) => 10
  (h/call-> 4 '(+ 1 2 3)) => 10
  (h/call-> 4 '(? even?)) => true)

(declare ops)
(facts "msg"
  (against-background (ops) => {:add (fn [_ & xs] (apply + xs))
                                :sub (fn [_ & xs] (apply - xs))})
  (h/msg (ops) :add) => 0
  (h/msg (ops) :add 1) => 1
  (h/msg (ops) :add 1 1) => 2
  (h/msg (ops) :add 1 1 1) => 3
  (h/msg (ops) :add 1 1 1 1) => 4
  (h/msg (ops) :sub 3) => -3
  (h/msg (ops) :sub 3 1) => 2
  (h/msg (ops) :sub 3 1 1) => 1
  (h/msg (ops) :sub 3 1 1 1) => 0)

(fact "make-??"
  (h/make-?? '+ '(1 2 3)) => '(list  (symbol "?") (quote +) 1 2 3))

(fact "??"
  (?? + 1 2 3) => '(? + 1 2 3))

(fact "make-?%"
  (h/make-?% '+ '(1 2 3)) => '(fn [?%] (+ ?% 1 2 3)))

(fact "?%"
  ((?% < 4) 3) => true
  ((?% + 1 2 3) 4) => 10)

(fact "eq-chk"
  (h/eq-chk 2 2) => true
  (h/eq-chk 2 even?) => true
  (h/eq-chk 2 (?% even?)) => true
  (h/eq-chk 2 (?? even?)) => true)

(fact "get-sel"
  (h/get-sel {"a" {:b {:c 1}}} "a") => {:b {:c 1}}
  (h/get-sel {:a {:b {:c 1}}} :a) => {:b {:c 1}}
  (h/get-sel {:a {:b {:c 1}}} [:a :b]) => {:c 1}
  (h/get-sel {:a {:b {:c 1}}} h/hash-map?) => true)

(fact "sel-chk"
  (h/sel-chk {:a {:b 1}} #(get % :a) {:b 1}) => true
  (h/sel-chk {"a" {:b 1}} "a" {:b 1}) => true
  (h/sel-chk {:a {:b 1}} :a h/hash-map?) => true
  (h/sel-chk {:a {:b 1}} :a {:b 1}) => true
  (h/sel-chk {:a {:b 1}} [:a :b] 1) => true)

(fact "sel-chk-all"
  (h/sel-chk-all {:a {:b 1}} [:a {:b 1} :a h/hash-map?]) => true)

(fact "eq-sel"
  (h/eq-sel 2 4 even?) => true
  (h/eq-sel 2 5 even?) => false
  (h/eq-sel 2 5 (?% > 3)) => false
  (h/eq-sel 2 5 (?% < 6)) => true
  (h/eq-sel {:id 1 :a 1} {:id 1 :a 2} h/hash-set?) => true
  (h/eq-sel {:id 1 :a 1} {:id 1 :a 2} :id) => true
  (h/eq-sel {:db {:id 1} :a 1} {:db {:id 1} :a 2} [:db :id]) => true)


(fact "eq-prchk"
  (h/eq-prchk {:a 1} :a) => true
  (h/eq-prchk {:a 1} h/hash-map?) => true
  (h/eq-prchk {:a 1} h/hash-set?) => false
  (h/eq-prchk {:a 1 :val 1} #(= 1 (% :val))) => true
  (h/eq-prchk {:a 1 :val 1} #(= 2 (% :val))) => false
  (h/eq-prchk {:a 1 :val 1} [:val 1]) => true
  (h/eq-prchk {:a 1 :val 1} [:val even?]) => false
  (h/eq-prchk {:a 1 :val 1} [:val (?% = 1)]) => true
  (h/eq-prchk {:a 1 :val 1} [:val (?% not= 1)]) => false
  (h/eq-prchk {:a 1 :val 1} [:val (?? = 1)]) => true
  (h/eq-prchk {:a 1 :val 1} [:val (?? not= 1)]) => false
  (h/eq-prchk {:a {:b 1}} [[:a :b] odd?]) => true
  (h/eq-prchk {:a {:b 1}} [[:a :b] (?? = 1) [:a] associative?]) => true)

(fact "suppress-prchk"
  (h/suppress-prchk "3" even?) => nil
  (h/suppress-prchk 3 even?) => nil
  (h/suppress-prchk 2 even?) => true)

(fact "queue"
  (h/queue 1 2 3 4) => [1 2 3 4]
  (pop (h/queue 1 2 3 4)) => [2 3 4])

(fact "uuid"
  (h/uuid) => h/uuid?
  (h/uuid "00000000-0000-0000-0000-000000000000") => h/uuid?
  (h/uuid 0 0) => h/uuid?)

(fact "uri"
  (h/uri "http://www.google.com") => h/uri?
  (h/uri "ssh://github.com") => h/uri?)

(fact "instant"
  (h/instant) => h/instant?
  (h/instant 0) => h/instant?)

(fact "type-predicates"
  (h/boolean? true) => true
  (h/boolean? false) => true
  (h/hash-map? {}) => true
  (h/hash-set? #{}) => true
  (h/long? 1) => true
  (h/long? 1N) => false
  (h/bigint? 1N) => true
  (h/bigdec? 1M) => true
  (h/instant? (h/instant 0)) => true
  (h/uuid? (h/uuid)) => true
  (h/bytes? (byte-array 8)) => true)

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

(fact "promise?"
  (h/promise? (future (inc 1))) => true)

(fact "type-checker"
  (h/type-checker :string) => (exactly #'clojure.core/string?)
  (h/type-checker :bytes) =>  (exactly #'hara.common/bytes?)
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

(fact "hash-keyword"
  (h/hash-keyword 1) => :__1__
  (h/hash-keyword 1 "id") => :__id_1__
  (h/hash-keyword 1 "1") => :__1_1__
  (h/hash-keyword "hello") => :__99162322__)


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
                :dtor seq}]) => [[1] {[2] [3]}]
  (h/manipulate #(* % 2) {1 [2 3] #{4 5} 6 7 '(8 (9 (10)))}) => {1 [4 6], #{4 5} 12, 7 '(16 (18 (20)))})

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

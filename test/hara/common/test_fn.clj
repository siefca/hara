(ns hara.common.test-fn
  (:require [hara.common.fn :as h]
            [hara.common.types :refer [hash-map? hash-set?]]
            [midje.sweet :refer :all]))

(facts "call "
  (h/call inc nil) => (throws Exception)
  (h/call inc 1) => 2
  (h/call + 1 1) => 2
  (h/call + 1 1 1 1 1 1) => 6
  (h/call nil) => nil
  (h/call nil 1 1 1) => nil)

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


(fact "make-exp"
  (h/make-exp 'x '(str)) => '(str x)
  (h/make-exp 'y '(str)) => '(str y)
  (h/make-exp 'x '((inc) (- 2) (+ 2)))
  => '(+ (- (inc x) 2) 2))

(fact "make-fn-exp"
  (h/make-fn-exp '(+ 2)) => '(fn [?%] (+ ?% 2)))

(fact "make-fn"
  ((h/fn-> '(+ 10)) 10) => 20)

(fact "call->"
  (h/call-> 4 [even?]) => true
  (h/call-> 4 '(< 3)) => false
  (h/call-> 4 (list #(< % 3))) => false
  (h/call-> 4 `(< 3)) => false
  (h/call-> 4 '(< 5)) => true
  (h/call-> 4 (list #(< % 5))) => true
  (h/call-> 4 `(< 5)) => true
  (h/call-> 4 `(+ 1 2 3)) => 10
  (h/call-> 4 '(+ 1 2 3)) => 10
  (h/call-> 4 '(even?)) => true
  (h/call-> 4 '((dec) (- 2) (+ 10))) => 11
  (h/call-> {:a {:b 1}} '((get-in [:a :b]) (= 1))) => true
  (h/call-> {:a {:b 1}} '([:a :b] (= 1))) => true)

(fact "check"
  (h/check 2 2) => true
  (h/check 2 even?) => true
  (h/check 2 '(even?)) => true
  (h/check {:a {:b 1}} '([:a :b] = 1)) => true)

(fact "get->"
  (h/get-> 4 even?) => true
  (h/get-> 4 'even?) => true
  (h/get-> {:a 1} '(:a (= 1))) => true
  (h/get-> {"a" {:b {:c 1}}} "a") => {:b {:c 1}}
  (h/get-> {:a {:b {:c 1}}} :a) => {:b {:c 1}}
  (h/get-> {:a {:b {:c 1}}} [:a :b]) => {:c 1}
  (h/get-> {:a {:b {:c 1}}} hash-map?) => true)

(fact "check->"
  (h/check-> {:a {:b 1}} #(get % :a) {:b 1}) => true
  (h/check-> {"a" {:b 1}} "a" {:b 1}) => true
  (h/check-> {:a {:b 1}} :a hash-map?) => true
  (h/check-> {:a {:b 1}} :a {:b 1}) => true
  (h/check-> {:a {:b 1}} [:a :b] 1) => true)

(fact "check-all->"
  (h/check-all-> {:a {:b 1}} [:a {:b 1} :a hash-map?]) => true)

(fact "eq->"
  (h/eq-> 2 4 even?) => true
  (h/eq-> 2 5 even?) => false
  (h/eq-> 2 5 '(> 3)) => false
  (h/eq-> 2 5 '(< 6)) => true
  (h/eq-> {:id 1 :a 1} {:id 1 :a 2} hash-set?) => true
  (h/eq-> {:id 1 :a 1} {:id 1 :a 2} :id) => true
  (h/eq-> {:db {:id 1} :a 1} {:db {:id 1} :a 2} [:db :id]) => true)

(fact "pcheck->"
  (h/pcheck-> {:a 1} :a) => true
  (h/pcheck-> {:a 1} hash-map?) => true
  (h/pcheck-> {:a 1} hash-set?) => false
  (h/pcheck-> {:a 1 :val 1} #(= 1 (% :val))) => true
  (h/pcheck-> {:a 1 :val 1} #(= 2 (% :val))) => false
  (h/pcheck-> {:a 1 :val 1} [:val 1]) => true
  (h/pcheck-> {:a 1 :val 1} [:val even?]) => false
  (h/pcheck-> {:a 1 :val 1} [:val '(= 1)]) => true
  (h/pcheck-> {:a 1 :val 1} [:val '(not= 1)]) => false
  (h/pcheck-> {:a {:b 1}} [[:a :b] odd?]) => true
  (h/pcheck-> {:a {:b 1}} [[:a :b] '(= 1) [:a] associative?]) => true)

(fact "suppress-pcheck"
  (h/suppress-pcheck "3" even?) => nil
  (h/suppress-pcheck 3 even?) => nil
  (h/suppress-pcheck 2 even?) => true)
  
(fact "arg-count"
  (h/arg-counts (fn [])) => '(0)
  (h/arg-counts (fn ([]) ([x]) ([x y]))) => (just 0 1 2 :in-any-order)
  (h/arg-counts (fn [& more])) => '())
  
(fact "varg-count"
 (h/varg-count (fn [& more])) => 0
 (h/varg-count (fn [a b c & more])) => 3)
 
(fact "op"
  (h/op (fn [a b] (+ a b)) 1 2 3 4) => 3)

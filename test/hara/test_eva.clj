(ns hara.test-eva
  (:refer-clojure :exclude [swap! reset!])
  (:use midje.sweet
        [hara.fn :only [deref*]]
        [hara.data.evom :only [evom swap! reset! add-watches remove-watches]])
  (:require [hara.eva :as v] :reload))

(defn is-evom [& [value]]
  (fn [evm]
    (if (and (instance? hara.data.Evom evm)
             (= @evm value))
      true)))

(defn is-eva [& values]
  (fn [ev]
    (if (and (instance? hara.data.Eva ev)
             (= (seq (persistent! ev)) values))
      true)))

(declare ev)

(facts "checking that the watches and the validators work"
  (against-background
    (before :checks
            (do
              (def ev (v/eva))
              (conj! ev 1))))

  (fact "initial watches should be 0"
    (v/get-elem-watches ev) => {})

  (fact "adding a watch and getting its value"
    (let [b (v/add-elem-watch ev :a identity)]
      (keys (v/get-elem-watches ev)))
    => '(:a))

  (fact "removing a watch and getting its value"
    (let [b (v/add-elem-watch ev :a identity)]
      (assert (= '(:a) (keys (v/get-elem-watches ev))))
      (v/remove-elem-watch ev :a)
      (v/get-elem-watches ev))
    => {}))

(facts "match? will return true if either the value agrees or the function returns true"
  (#'v/match? {:id 1} :id 1) => truthy
  (#'v/match? {:id 1} :id odd?) => truthy
  (#'v/match? {:id 1} :id 2) => falsey
  (#'v/match? {:id 1} :id even?) => falsey)

(facts "all-match? will only return true if all the key/check pairs match"
  (#'v/all-match? {:id 1 :val 1} []) => true
  (#'v/all-match? {:id 1 :val 1} [:id 1]) => true
  (#'v/all-match? {:id 1 :val 1} [:val 1]) => true
  (#'v/all-match? {:id 1 :val 1} [:id odd? :val 1]) => true
  (#'v/all-match? {:id 1 :val 1} [:id 1 :val odd?]) => true
  (#'v/all-match? {:id 1 :val 1} [:id odd? :val odd?]) => true
  (#'v/all-match? {:id 1 :val 1} [:id 2]) => falsey
  (#'v/all-match? {:id 1 :val 1} [:val 2]) => falsey
  (#'v/all-match? {:id 1 :val 1} [:id even? :val 1]) => falsey
  (#'v/all-match? {:id 1 :val 1} [:id 1 :val even?]) => falsey
  (#'v/all-match? {:id 1 :val 1} [:id 2 :val even?]) => falsey
  (#'v/all-match? {:id 1 :val 1} [:id even? :val even?]) => falsey)

(facts "rm-indices will remove indices from a vector"
  (#'v/rm-indices [] []) => []
  (#'v/rm-indices [0] []) => [0]
  (#'v/rm-indices [0] [0]) => []
  (#'v/rm-indices [0 1 2 3 4 5] [0]) => [1 2 3 4 5]
  (#'v/rm-indices [0 1 2 3 4 5] []) => [0 1 2 3 4 5]
  (#'v/rm-indices [0 1 2 3 4 5] [0 2 4]) => [1 3 5]
  (#'v/rm-indices [0 1 2 3 4 5] [1 3 5]) => [0 2 4]
  (#'v/rm-indices [0 1 2 3 4 5] [0 1 2 3 4 5]) => [])


(facts "indices will grab the necessary indices"
  (against-background
    (before :checks
            (def ev (v/eva [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))))

  (fact
    (v/indices ev) => (throws Exception)
    (v/indices ev 0) => [0]
    (v/indices ev 2) => [2]
    (v/indices ev #{1 2}) => [1 2]
    (v/indices ev #{0}) => [0]
    (v/indices ev #{4}) => []
    (v/indices ev #(odd? (:id %))) => [0 2]
    (v/indices ev #(even? (:val %))) => [2 3]
    (v/indices ev [:id 1]) => [0]
    (v/indices ev [:val 1]) => [0 1]
    (v/indices ev [:val nil?]) => []
    (v/indices ev [:id odd?]) => [0 2]
    (v/indices ev [:val even?]) => [2 3]
    (v/indices ev [:val even? :id odd?]) => [2]))


(facts "select will grab the necessary entries"
  (against-background
    (before :checks
            (def ev (v/eva [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))))

  (fact
    (v/select ev) => [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]
    (v/select ev 0) => [{:id 1 :val 1}]
    (v/select ev 2) => [{:id 3 :val 2}]
    (v/select ev #{1 2}) => [{:id 2 :val 1} {:id 3 :val 2}]
    (v/select ev #{0}) => [{:id 1 :val 1}]
    (v/select ev #{4}) => []
    (v/select ev 2) => [{:id 3 :val 2}]
    (v/select ev #(odd? (:id %))) => [{:id 1 :val 1} {:id 3 :val 2}]
    (v/select ev #(even? (:val %))) => [{:id 3 :val 2} {:id 4 :val 2}]
    (v/select ev [:id 1]) => [{:id 1 :val 1}]
    (v/select ev [:val 1]) => [{:id 1 :val 1} {:id 2 :val 1}]
    (v/select ev [:val nil?]) => []
    (v/select ev [:id odd?]) => [{:id 1 :val 1} {:id 3 :val 2}]
    (v/select ev [:val even?]) => [{:id 3 :val 2} {:id 4 :val 2}]
    (v/select ev [:val even? :id odd?]) => [{:id 3 :val 2}]))

(facts "update will operate on maps only"
  (against-background
    (before :checks
            (def ev (v/eva [{:id 1 :val 1} {:id 2 :val 2} 0]))))

  (v/update! ev 0 {:val 2}) => (is-eva {:id 1 :val 2} {:id 2 :val 2} 0)
  (v/update! ev 1 {:id 3 :val 3 :valb 4}) => (is-eva {:id 1 :val 1} {:id 3 :val 3 :valb 4} 0)
  (v/update! ev 2) => (throws Exception)
  (v/update! ev [:id 1] {:val 2}) => (throws Exception))

(facts "update using array checks"
  (against-background
    (before :checks
            (def ev (v/eva [{:id 1 :val 1} {:id 2 :val 2}]))))

  (v/update! ev [:id 1] {:val 2}) => (is-eva {:id 1 :val 2} {:id 2 :val 2})
  (v/update! ev [:val 2] {:val 3}) => (is-eva {:id 1 :val 1} {:id 2 :val 3}))

(facts "update using array checks"
  (against-background
    (before :checks
            (def ev (v/eva [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))))

  (v/update! ev [:id 1] {:val 2}) => (is-eva {:id 1 :val 2} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (v/update! ev [:val 2] {:val 3}) => (is-eva {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 3})
  (v/update! ev #(odd? (:id %)) {:val 3}) => (is-eva {:id 1 :val 3} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 2})
  (v/update! ev #(even? (:val %)) {:val 4}) => (is-eva {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 4} {:id 4 :val 4})
  (v/update! ev #{1 2} {:val 4}) => (is-eva {:id 1 :val 1} {:id 2 :val 4} {:id 3 :val 4} {:id 4 :val 2})
  (v/update! ev #{0} {:val 4}) => (is-eva {:id 1 :val 4} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (v/update! ev #{4} {:val 4}) => (is-eva {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (v/update! ev [:id odd?] {:val 3}) => (is-eva {:id 1 :val 3} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 2})
  (v/update! ev [:val even?] {:val 4}) => (is-eva {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 4} {:id 4 :val 4})
  (v/update! ev [:id odd? :val even?] {:valb 4}) => (is-eva {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2 :valb 4} {:id 4 :val 2}))

(facts "replace is like update"
  (against-background
    (before :checks
            (def ev (v/eva [1 2 3 4 5 6]))))
  (v/replace! ev 0 0) => (is-eva 0 2 3 4 5 6)
  (v/replace! ev #{1 2} 0) => (is-eva 1 0 0 4 5 6)
  (v/replace! ev odd? 0) => (is-eva 0 2 0 4 0 6)
  (v/replace! ev [:id 1] 0) => (throws Exception)
  (v/replace! (v/eva [{:id 1 :val 1} {:id 2 :val 1}]) [:id 1] 0) => (is-eva 0 {:id 2 :val 1}))


(facts "delete using array checks"
  (against-background
    (before :checks
            (def ev (v/eva [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))))

  (v/delete! ev [:id 1]) => (is-eva {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (v/delete! ev [:val 2]) => (is-eva {:id 1 :val 1} {:id 2 :val 1})
  (v/delete! ev #(odd? (:id %))) => (is-eva {:id 2 :val 1} {:id 4 :val 2})
  (v/delete! ev #(even? (:val %))) => (is-eva {:id 1 :val 1} {:id 2 :val 1})
  (v/delete! ev #{1 2}) => (is-eva {:id 1 :val 1} {:id 4 :val 2})
  (v/delete! ev #{0}) => (is-eva {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (v/delete! ev #{4}) => (is-eva {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (v/delete! ev [:id odd?]) => (is-eva  {:id 2 :val 1} {:id 4 :val 2})
  (v/delete! ev [:val even?]) => (is-eva {:id 1 :val 1} {:id 2 :val 1})
  (v/delete! ev [:id odd? :val even?]) => (is-eva {:id 1 :val 1} {:id 2 :val 1} {:id 4 :val 2}))

(facts "insert puts a new object"
  (against-background
    (before :checks
            (def ev (v/eva [{:id 1 :val 1}]))))
  (v/insert! ev {:id 2 :val 1}) => (is-eva {:id 1 :val 1} {:id 2 :val 1})
  (v/insert! ev {:id 2 :val 1} 0) => (is-eva {:id 2 :val 1} {:id 1 :val 1})
  (v/insert! ev {:id 2 :val 1} 1) => (is-eva {:id 1 :val 1} {:id 2 :val 1})
  (v/insert! ev {:id 2 :val 1} -1) => (throws Exception)
  (v/insert! ev {:id 2 :val 1} 2) => (throws Exception)
  (-> ev
      (v/insert! {:id 3 :val 1} 1)
      (v/insert! {:id 2 :val 1} 1)) => (is-eva {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 1})))

;; TODO:

;; - Tests and implementation for insert!
;; - Test how watches behave when elements are added (insert) and removed (delete) from Eva
;; - Add in 'install idx watch, which adds a function that allows the eva to see all manipulations to its evom



(def ev (v/eva [{:id 1 :val 1} {:id 2 :val 2} 0]))
(v/update! ev 0 {:val 2})

(swap! (@ev 0) #(into % {:val 2}))

;;(println ev)
(v/map! (v/eva [1 2 3 4]) inc)

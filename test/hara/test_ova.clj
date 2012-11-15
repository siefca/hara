(ns hara.test-ova
  (:use midje.sweet
        [hara.fn :only [deref*]])
  (:require [hara.ova :as v] :reload))

(defn is-atom [& [value]]
  (fn [at]
    (if (and (instance? clojure.lang.Atom at)
             (= @at value))
      true)))

(defn is-ref [& [value]]
  (fn [rf]
    (if (and (instance? clojure.lang.Ref rf)
             (= @rf value))
      true)))

(defn is-ova [& values]
  (fn [ov]
    (if (and (instance? hara.data.Ova ov)
             (= (seq (persistent! ov)) values))
      true)))

(declare ov)

(facts "checking that the watches and the validators work"
  (against-background
    (before :checks
            (do
              (def ov (v/ova))
              (dosync (conj! ov 1)))))

  (fact "initial watches should be 0"
    (v/get-elem-watches ov) => {})

  (fact "adding a watch and getting its value"
    (let [b (v/add-elem-watch ov :a identity)]
      (keys (v/get-elem-watches ov)))
    => '(:a))

  (fact "removing a watch and getting its value"
    (let [b (v/add-elem-watch ov :a identity)]
      (assert (= '(:a) (keys (v/get-elem-watches ov))))
      (v/remove-elem-watch ov :a)
      (v/get-elem-watches ov))
    => {}))

(facts "match? will return true if either the value agrees or the function returns true"
  (#'v/match? {:id 1} :id 1) => truthy
  (#'v/match? {:id 1} :id odd?) => truthy
  (#'v/match? {:id 1} :id 2) => falsey
  (#'v/match? {:id 1} :id even?) => falsey
  (#'v/match? {:id {:a 1}} [:id :a] 1) => truthy
  (#'v/match? {:id {:a 1}} [:id :a] odd?) => truthy
  (#'v/match? {:id {:a 1}} [:id :a] 2) => falsey
  (#'v/match? {:id {:a 1}} [:id :a] even?) => falsey
  (#'v/match? {:id {:a 1}} [:id :b] 1) => falsey
  (#'v/match? {:id {:a 1}} [:id :b] odd?) => falsey
 )

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
  (#'v/all-match? {:id {:a 1} :val 1} [[:id :a] even? :val even?]) => falsey
  (#'v/all-match? {:id {:a 1} :val 1} [[:id :a] 1 :val even?]) => falsey
  (#'v/all-match? {:id {:a 1} :val 1} [[:id :a] 2 :val even?]) => falsey
  (#'v/all-match? {:id {:a 1} :val 1} [[:id :a] even? :val even?]) => falsey)

(facts "indices will grab the necessary indices"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2} {:id {:val 5}}]))))

  (fact
    (v/indices ov) => (throws Exception)
    (v/indices ov 0) => [0]
    (v/indices ov 2) => [2]
    (v/indices ov #{1 2}) => [1 2]
    (v/indices ov #{0}) => [0]
    (v/indices ov #{4}) => [4]
    (v/indices ov #(odd? (:id %))) => [0 2]
    (v/indices ov #(even? (:val %))) => [2 3]
    (v/indices ov [:id 1]) => [0]
    (v/indices ov [:val 1]) => [0 1]
    (v/indices ov [:val nil?]) => [4]
    (v/indices ov [:id odd?]) => [0 2]
    (v/indices ov [:val even?]) => [2 3]
    (v/indices ov [:val even? :id odd?]) => [2]))


(facts "select will grab the necessary entries"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))))

  (fact
    (v/select ov) => [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]
    (v/select ov 0) => [{:id 1 :val 1}]
    (v/select ov 2) => [{:id 3 :val 2}]
    (v/select ov #{1 2}) => [{:id 2 :val 1} {:id 3 :val 2}]
    (v/select ov #{0}) => [{:id 1 :val 1}]
    (v/select ov #{4}) => []
    (v/select ov 2) => [{:id 3 :val 2}]
    (v/select ov #(odd? (:id %))) => [{:id 1 :val 1} {:id 3 :val 2}]
    (v/select ov #(even? (:val %))) => [{:id 3 :val 2} {:id 4 :val 2}]
    (v/select ov [:id 1]) => [{:id 1 :val 1}]
    (v/select ov [:val 1]) => [{:id 1 :val 1} {:id 2 :val 1}]
    (v/select ov [:val nil?]) => []
    (v/select ov [:id odd?]) => [{:id 1 :val 1} {:id 3 :val 2}]
    (v/select ov [:val even?]) => [{:id 3 :val 2} {:id 4 :val 2}]
    (v/select ov [:val even? :id odd?]) => [{:id 3 :val 2}]))

(facts "update will operate on maps only"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 2} 0]))))

  (dosync (v/update! ov 0 {:val 2})) => (is-ova {:id 1 :val 2} {:id 2 :val 2} 0)
  (dosync (v/update! ov 1 {:id 3 :val 3 :valb 4})) => (is-ova {:id 1 :val 1} {:id 3 :val 3 :valb 4} 0)
  (dosync (v/update! ov 2)) => (throws Exception)
  (dosync (v/update! ov [:id 1] {:val 2})) => (is-ova {:id 1 :val 2} {:id 2 :val 2} 0))

(facts "update using array checks"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 2}]))))

  (dosync (v/update! ov [:id 1] {:val 2})) => (is-ova {:id 1 :val 2} {:id 2 :val 2})
  (dosync (v/update! ov [:val 2] {:val 3})) => (is-ova {:id 1 :val 1} {:id 2 :val 3}))

(facts "update using array checks"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))))

  (dosync (v/update! ov [:id 1] {:val 2})) => (is-ova {:id 1 :val 2} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (dosync (v/update! ov [:val 2] {:val 3})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 3})
  (dosync (v/update! ov #(odd? (:id %)) {:val 3})) => (is-ova {:id 1 :val 3} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 2})
  (dosync (v/update! ov #(even? (:val %)) {:val 4})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 4} {:id 4 :val 4})
  (dosync (v/update! ov #{1 2} {:val 4})) => (is-ova {:id 1 :val 1} {:id 2 :val 4} {:id 3 :val 4} {:id 4 :val 2})
  (dosync (v/update! ov #{0} {:val 4})) => (is-ova {:id 1 :val 4} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (dosync (v/update! ov #{4} {:val 4})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (dosync (v/update! ov [:id odd?] {:val 3})) => (is-ova {:id 1 :val 3} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 2})
  (dosync (v/update! ov [:val even?] {:val 4})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 4} {:id 4 :val 4})
  (dosync (v/update! ov [:id odd? :val even?] {:valb 4})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2 :valb 4} {:id 4 :val 2}))

(facts "replace is like update but replaces the entire cell"
  (against-background
    (before :checks
            (def ov (v/ova [1 2 3 4 5 6]))))
  (dosync (v/replace! ov 0 0)) => (is-ova 0 2 3 4 5 6)
  (dosync (v/replace! ov #{1 2} 0)) => (is-ova 1 0 0 4 5 6)
  (dosync (v/replace! ov odd? 0)) => (is-ova 0 2 0 4 0 6)
  (dosync (v/replace! ov [:id 1] 0)) => (is-ova 1 2 3 4 5 6)
  (dosync (v/replace! (v/ova [{:id 1 :val 1} {:id 2 :val 1}]) [:id 1] 0)) => (is-ova 0 {:id 2 :val 1}))

(facts "update-in!"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val {:a 1}}]))))
  (dosync (v/update-in! ov [:id 1] [:val :x] (constantly 2))) => (is-ova {:id 1 :val {:a 1 :x 2}})
  (dosync (v/update-in! ov [:id 1] [:val :a] (constantly 2))) => (is-ova {:id 1 :val {:a 2}}))

(facts "replace-in!"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val {:a 1}}]))))
  (dosync (v/replace-in! ov [:id 1] [:val :x] 2)) => (is-ova {:id 1 :val {:a 1 :x 2}})
  (dosync (v/replace-in! ov [:id 1] [:val :a] 2)) => (is-ova {:id 1 :val {:a 2}}))

(facts "delete using array checks"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))))

  (dosync (v/delete! ov [:id 1])) => (is-ova {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (dosync (v/delete! ov [:val 2])) => (is-ova {:id 1 :val 1} {:id 2 :val 1})
  (dosync (v/delete! ov #(odd? (:id %)))) => (is-ova {:id 2 :val 1} {:id 4 :val 2})
  (dosync (v/delete! ov #(even? (:val %)))) => (is-ova {:id 1 :val 1} {:id 2 :val 1})
  (dosync (v/delete! ov #{1 2})) => (is-ova {:id 1 :val 1} {:id 4 :val 2})
  (dosync (v/delete! ov #{0})) => (is-ova {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (dosync (v/delete! ov #{4})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
  (dosync (v/delete! ov [:id odd?])) => (is-ova  {:id 2 :val 1} {:id 4 :val 2})
  (dosync (v/delete! ov [:val even?])) => (is-ova {:id 1 :val 1} {:id 2 :val 1})
  (dosync (v/delete! ov [:id odd? :val even?])) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 4 :val 2}))

(facts "insert puts a new object"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val 1}]))))
  (dosync (v/insert! ov {:id 2 :val 1})) => (is-ova {:id 1 :val 1} {:id 2 :val 1})
  (dosync (v/insert! ov {:id 2 :val 1} 0)) => (is-ova {:id 2 :val 1} {:id 1 :val 1})
  (dosync (v/insert! ov {:id 2 :val 1} 1)) => (is-ova {:id 1 :val 1} {:id 2 :val 1})
  (dosync (v/insert! ov {:id 2 :val 1} -1)) => (throws Exception)
  (dosync (v/insert! ov {:id 2 :val 1} 2)) => (throws Exception)
  (dosync (-> ov
      (v/insert! {:id 3 :val 1} 1)
      (v/insert! {:id 2 :val 1} 1))) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 1}))

(fact "reverse!"
  (dosync (v/reverse! (v/ova [1 2 3 4 5]))) => (is-ova 5 4 3 2 1))

(fact "filter!"
  (dosync (v/filter! (v/ova [1 2 3 4 5 6 7]) odd?)) => (is-ova 1 3 5 7))

(fact "sort!"
  (dosync (v/sort! (v/ova [3 2 1 5 4 7 6]))) => (is-ova 1 2 3 4 5 6 7))



;; Watch functionality
(facts "testing the add-elem-watch function with map"
  (let [ov     (v/ova [1 2 3 4])
        out    (atom [])
        cj-fn  (fn  [o r k p v & args]
                 ;;(println o r k p v args )
                 (swap! out conj [p v]))
        _      (v/add-elem-watch ov :conj cj-fn)
        _      (dosync (v/map! ov inc))]
    (facts "out is updated"
      ov => (is-ova 2 3 4 5)
      (sort @out) => [[1 2] [2 3] [3 4] [4 5]])))

(facts "testing the add-elem-watch function when an element has been deleted"
  (let [ov     (v/ova [1 2 3 4])
        out    (atom [])
        cj-fn  (fn  [_ _ _ p v & args]
                 (swap! out conj [p v]))
        _      (v/add-elem-watch ov :conj cj-fn)
        _      (dosync (v/delete! ov 0))
        _      (dosync (v/map! ov inc))]
    (facts "out is updated"
      ov => (is-ova 3 4 5)
      (sort @out) => [[2 3] [3 4] [4 5]])))

(facts "testing the add-elem-watch function when an element has been added"
  (let [ov     (v/ova [1 2 3 4])
        out    (atom [])
        cj-fn  (fn  [_ _ _ p v & args]
                 (swap! out conj [p v]))
        _      (v/add-elem-watch ov :conj cj-fn)
        _      (dosync (v/insert! ov 3))
        _      (dosync (v/map! ov inc))]
    (facts "out is updated"
      ov => (is-ova 2 3 4 5 4))))

(facts "testing the add-elem-watch function when an elements are arbitrarily "
  (let [ov     (v/ova [1 2 3 4])
        out    (atom [])
        cj-fn  (fn  [_ _ _ p v & args]
                 (swap! out conj [p v]))
        _      (v/add-elem-watch ov :conj cj-fn)]

    (dosync (v/insert! ov 1 3))
    (fact ov => (is-ova 1 2 3 1 4))

    (dosync (v/delete! ov odd?))
    (fact ov => (is-ova 2 4))

    (dosync (v/map! ov inc))
    (fact ov => (is-ova 3 5))

    (dosync (v/concat! ov [1 2 3 4 5]))
    (fact ov  => (is-ova 3 5 1 2 3 4 5))

    (dosync (v/sort! ov))
    (fact ov  => (is-ova 1 2 3 3 4 5 5))

     ;; [3 5 1 2 3 4 5]
    ;;
    ;;(dosync (fact  => (is-ova 1 2 3 3 4 5 5)))
    ;;(dosync (fact (v/map-indexed! ov (fn [i x] x)) => (is-ova 1 2 3 3 4 5 5)))
    ;;(fact (dosync (v/map-indexed! ov (fn [i x] i))) => (is-ova 0 1 2 3 4 5 6))

    ))
(comment
  (def a (v/ova [5 4 3 2 6]))
  (println a)
  (dosync (v/sort! a)))
;;(def a (v/ova [1 1 2 3 4 5]))
;;(v/map-indexed! a (fn [i x] i))
;;(count a)

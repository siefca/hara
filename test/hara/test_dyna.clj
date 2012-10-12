(ns hara.test-dyna
  (:use midje.sweet
        hara.data.iotam)
  (:require [hara.data.dyna :as d] :reload))

(def ^:dynamic s* d/search)

(def my-dka (hara.data.dyna/new [{:id :1 :val 1}, {:id :2 :val 2}, {:id :3 :val 3}]))

(background
 (before :checks
         (def my-dka (hara.data.dyna/new [{:id :1 :val 1}, {:id :2 :val 2}, {:id :3 :val 3}])))

 (before :checks
         (def my-dkb
           (let [dy (d/new)]
             (dosync
              (alter
               (:data (.state dy))
               (fn [_]
                 {:1 (atom {:id :1 :val 1})
                  :2 (atom {:id :2 :val 2})
                  :3 (atom {:id :3 :val 3})})))
             dy)))

 (before :checks
         (def my-dx (hara.data.dyna/new [{:id :id :a {:a :a :b {:b :b :c :c}}}]))))


(fact "ids will output all the ids of the map"
  (apply hash-set (d/ids my-dka)) => #{:1 :2 :3})

(fact "has-id? checks to see if the given key is in the "
  (d/has-id? my-dka :1) => true
  (d/has-id? my-dka :0) => false)

(fact "ch-id? will change the id of an entry, for admin purposes"
  (d/ch-id! my-dka :0 :1) => (throws AssertionError)
  (d/ch-id! my-dka :2 :1) => (throws AssertionError)
  (s* (d/ch-id! my-dka :1 :1)) => (s* my-dka)
  (s* (d/ch-id! my-dka :1 :0)) => [{:id :0 :val 1}, {:id :2 :val 2}, {:id :3 :val 3}])

(fact "count returns the number of items"
  (count my-dka) => 3)

;;(s* my-dkb)
(fact "select or s* will select elements based on the predicate and order them using a comparator"
  (s* my-dka) => (s* my-dkb)
  (s* my-dka :1) => [{:id :1 :val 1}]

  (s* my-dka #(not (= :1 (:id %)))) => [{:id :2 :val 2} {:id :3 :val 3}]

  (s* my-dka #(not (= :1 (:id %)))
      (fn [x y] (.compareTo (:id y) (:id x))))
  => [{:id :3 :val 3} {:id :2 :val 2}]

  (s* my-dka #(keyword? (:id %))) => (s* my-dkb)
  (s* my-dka #(= 2 (:val %))) => [{:id :2 :val 2}])

(fact "empty! will remove everything in the list"
  (s* (d/empty! my-dka)) => []
  (s* (d/empty! my-dkb)) => [])

(fact "delete! will remove the item from the deck "
  (d/delete! my-dka :0) => (throws AssertionError)
  (s* (d/delete! my-dka :1)) => [{:id :2 :val 2}, {:id :3 :val 3}])

(fact "insert will add an entry with the given id, or throws an exception if it is already there"
  ;(s* (d/insert! my-dx  {:id :id :val 0})) => [{:id :id :val 0}]
  ;(s* (d/insert! my-dka {:id :1 :val 1})) => [{:id :1 :val 1}, {:id :2 :val 2}, {:id :3 :val 3}]
  ;(s* (d/insert! my-dka {:id :1 :val 2})) => [{:id :1 :val 2}, {:id :2 :val 2}, {:id :3 :val 3}]
  (s* (d/insert! my-dx  {:id :id :val 0})) => (throws AssertionError)
  (s* (d/insert! my-dka {:id :1 :val 1})) => (throws AssertionError)
  (s* (d/insert! my-dka {:id :1 :val 2})) => (throws AssertionError))

(fact "update will add to an entry with the given id or insert if it is not there"
  (s* (d/update! my-dx  {:id :id :val 0})) => [{:id :id :val 0 :a {:a :a :b {:b :b :c :c}}}]
  (s* (d/update! my-dka {:id :1 :val 1})) => [{:id :1 :val 1}, {:id :2 :val 2}, {:id :3 :val 3}]
  (s* (d/update! my-dka {:id :1 :val 2})) => [{:id :1 :val 2}, {:id :2 :val 2}, {:id :3 :val 3}])

(fact "op! will perform an operation on the given id"
  (s* (d/op! my-dka :1 assoc :val 2)) => [{:id :1 :val 2}, {:id :2 :val 2}, {:id :3 :val 3}])

(fact "op-pred! will perform an operation on the the predicates"
  (s* (d/op-pred! my-dka #(not (= :1 (:id %))) assoc :val 1)) => [{:id :1 :val 1}, {:id :2 :val 1}, {:id :3 :val 1}])

(fact "op-all! will perform an operation on the entire deck"
  (s* (d/op-all! my-dka assoc :val 0)) => [{:id :1 :val 0}, {:id :2 :val 0}, {:id :3 :val 0}])

(fact "assoc-in! performs an assoc operation based upon the predicate"
 (s* (d/assoc-in! my-dka :1 :id 1)) => (throws AssertionError)
 (s* (d/assoc-in! my-dka :1 :val 2)) => [{:id :1 :val 2}, {:id :2 :val 2}, {:id :3 :val 3}]
 (s* (d/assoc-in! my-dka :1 :wal 2)) => [{:id :1 :val 1 :wal 2}, {:id :2 :val 2}, {:id :3 :val 3}]
 ;;(s* (d/assoc-in! my-dka #(not (= :1 (:id %))) :val 1)) => [{:id :1 :val 1}, {:id :2 :val 1}, {:id :3 :val 1}]
 )

(fact "dissoc-in! performs an dissoc operation based upon the predicate"
  (s* (d/dissoc-in! my-dka :1 :val)) => (throws AssertionError)
  ;;[{:id :1}, {:id :2 :val 2}, {:id :3 :val 3}]
  )

(fact "update-in! performs an dissoc operation based upon the predicate"
  (s* (d/update-in! my-dka :1 [:a :b] (fn [_] :c))) =>
  [{:id :1 :val 1 :a {:b :c}},
   {:id :2 :val 2},
   {:id :3 :val 3}])

(fact "! performs like the jquery $"
  (d/! my-dka :1 :val) => 1
  (d/! my-dka :1 :id) => :1
  (s* (d/! my-dka :1 :wal 2)) =>  [{:id :1 :val 1 :wal 2}, {:id :2 :val 2}, {:id :3 :val 3}])

;; Saving and Printing Methods
;;(d/save-deck my-dka "user1.clj")
;;(def a (d/load-deck "user1.clj"))
;;(println a)
(comment
  (d/add-elem-watch my-dka :a println)
  (d/get-elem-watches my-dka)
  (d/update-in! my-dka :1 [:a :b] (fn [_] :c))

  (println my-dka)
  (d/reset-in! my-dka :1 {:id :1 :val 1})
  (def a (my-dka :1))
  (ireset! a {:id :1 :val 1})
  (d/remove-elem-watch my-dka :a)

  (d/empty! my-dka)
  (ireset! a {:id :1 :val 1})

  (def my-dka (hara.data.dyna/new [{:id :1 :val 1}, {:id :2 :val 2}, {:id :3 :val 3}]))
)
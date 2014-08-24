(ns hara.ova-test
  (:use midje.sweet)
  (:require [hara.ova :refer :all]
            [hara.common.watch :as watch]))

^{:refer hara.ova/get-filtered :added "2.1"}
(fact "gets the first element in the ova that matches the selector:"

  (let [o (ova [{:id :1 :val 1} {:id :2 :val 1}])]
    (get-filtered o :1 nil nil)
    => {:val 1, :id :1}

    (get-filtered o :2 nil nil)
    => {:val 1, :id :2}

    (get-filtered o :3 nil :not-found)
    => :not-found))

^{:refer hara.ova/-invoke :added "2.1"}
(fact "function invocation finds the first value that matches the selector:"

  (let [o (ova [{:id :1 :val 1} {:id :2 :val 1}
                {:id :3 :val 2} {:id :4 :val 2}])]

    ;; Simplified indices and :id lookups
    (o 0)  => {:val 1, :id :1}
    (o :1) => {:val 1, :id :1}
    (:1 o) => {:val 1, :id :1}

    ;; Selector lookups
    (o :id :2) => {:val 1, :id :2}
    (o :val 2) => {:val 2, :id :3}
    (o :val even?) => {:val 2, :id :3}
    (o (list :id name) "4") => {:val 2, :id :4}))

^{:refer hara.ova/ova :added "2.1"}
(fact "constructs an ova instance"

  (ova []) ;=> #ova []
  (ova [1 2 3]) ;=>  #ova [1 2 3]
  (ova [{:id :1} {:id :2}]) ;=> #ova [{:id :1} {:id :2}]
  )

^{:refer hara.ova/concat! :added "2.1"}
(fact "works like clojure.core/concat, but modifies ova state"

  (let [o1 (ova [{:id :1 :val 1} {:id :2 :val 1}])
        o2 (ova [{:id :3 :val 2}])
        arr [{:id :4 :val 2}]]
    (dosync (concat! o1 o2 arr))
    (persistent! o1))
  => [{:val 1, :id :1} {:val 1, :id :2}
      {:val 2, :id :3} {:val 2, :id :4}])

^{:refer hara.ova/append! :added "2.1"}
(fact "like conj! but appends multiple array elements to the ova"

  (let [o (ova [{:id :1 :val 1}])]
    (dosync (append! o {:id :2 :val 1} {:id :3 :val 2}))
    (persistent! o))
  => [{:id :1 :val 1} {:id :2 :val 1} {:id :3 :val 2}])

^{:refer hara.ova/init! :added "2.1"}
(fact "re-initialises the ova to either an empty array or the second argument`coll`"

  (let [o (ova [])]
    (dosync (init! o [{:id :1 :val 1} {:id :2 :val 1}]))
    (persistent! o))
  => [{:val 1, :id :1} {:val 1, :id :2}])

^{:refer hara.ova/indices :added "2.1"}
(fact "provides intuitive filtering functionality of ova elements, outputting valid indices"

  (let [o (ova [{:id :1 :val 1} {:id :2 :val 1}
                {:id :3 :val 2} {:id :4 :val 2}])]
    (indices o) => [0 1 2 3]
    (indices o 0) => [0]
    (indices o [:val 1]) => [0 1]
    (indices o [:val even?]) => [2 3]
    (indices o [:val even? '(:id (name) (bigint)) odd?]) => [2])

  ^:hidden
  (let [o (ova [{:id :1 :val 1} {:id :2 :val 1}
                {:id :3 :val 2} {:id :4 :val 2}])]
    (indices o 2) => [2]
    (indices o #{1 2}) => [1 2]
    (indices o #{0}) => [0]
    (indices o #{4}) => []
    (indices o :1) => []
    (indices o [:val odd?]) => [0 1]
    (indices o #(even? (:val %))) => [2 3]
    (indices o [:id :1]) => [0]))

^{:refer hara.ova/select :added "2.1"}
(fact "grabs the selected ova entries as a set of values"

  (let [o (ova [{:id :1 :val 1} {:id :2 :val 1}
                {:id :3 :val 2} {:id :4 :val 2}])]

    (select o) => #{{:id :1, :val 1} {:id :2, :val 1} {:id :3, :val 2} {:id :4, :val 2}}
    (select o 0) => #{{:id :1 :val 1}}
    (select o #{1 2}) => #{{:id :2 :val 1} {:id :3 :val 2}}
    (select o #(even? (:val %))) => #{{:id :3 :val 2} {:id :4 :val 2}}
    (select o [:val 1]) => #{{:id :1 :val 1} {:id :2 :val 1}}
    (select o [:val even?]) => #{{:id :3 :val 2} {:id :4 :val 2}}
    (select o #{[:id :1] [:val 2]})
    => #{{:id :1 :val 1} {:id :3 :val 2} {:id :4 :val 2}}
    (select o [:id '((name) (bigint) (odd?))])
    => #{{:id :1 :val 1} {:id :3 :val 2}}))

^{:refer hara.ova/has? :added "2.1"}
(fact "checks that the ova contains elements matching a selector"

  (let [o (ova [{:id :1 :val 1} {:id :2 :val 1}
                {:id :3 :val 2} {:id :4 :val 2}])]

    (has? o) => true
    (has? o 0) => true
    (has? o -1) => false
    (has? o [:id '((name) (bigint) (odd?))]) => true))

^{:refer hara.ova/map! :added "2.1"}
(fact "applies a function on the ova with relevent arguments"

  (let [o (ova [{:id :1} {:id :2}])]
    (dosync (map! o assoc :val 1))
    (persistent! o))
  => [{:val 1, :id :1} {:val 1, :id :2}])

^{:refer hara.ova/map-indexed! :added "2.1"}
(fact "applies a function that taking the data index as well as the data
  to all elements of the ova"

  (let [o (ova [{:id :1} {:id :2}])]
    (dosync (map-indexed! o (fn [i m]
                              (assoc m :val i))))
    (persistent! o))
  => [{:val 0, :id :1} {:val 1, :id :2}])

^{:refer hara.ova/smap! :added "2.1"}
(fact "applies a function to only selected elements of the array"

  (let [o (ova [{:id :1 :val 1} {:id :2 :val 1}
                {:id :3 :val 2} {:id :4 :val 2}])]
    (dosync (smap! o [:val 1]
                   update-in [:val] #(+ % 100)))
    (persistent! o))
  => [{:val 101, :id :1} {:val 101, :id :2} {:val 2, :id :3} {:val 2, :id :4}])

^{:refer hara.ova/smap-indexed! :added "2.1"}
(fact "applies a function that taking the data index as well as the data
  to selected elements of the ova"

  (let [o (ova [{:id :1 :val 1} {:id :2 :val 1}
                {:id :3 :val 2} {:id :4 :val 2}])]
    (dosync (smap-indexed! o [:val 1]
                           (fn [i m]
                             (update-in m [:val] #(+ i 100 %)))))
    (persistent! o))
  => [{:val 101, :id :1} {:val 102, :id :2} {:val 2, :id :3} {:val 2, :id :4}])

^{:refer hara.ova/insert! :added "2.1"}
(fact "inserts data at either the end of the ova or when given an index"

  (let [o (ova (range 5))]
    (dosync (insert! o 6))
    (dosync (insert! o 5 5))
    (persistent! o))
  => [0 1 2 3 4 5 6])

^{:refer hara.ova/sort! :added "2.1"}
(fact "sorts all data in the ova using a comparator function"

  (let [o (ova [2 1 3 4 0])]
    (dosync (sort! o >))
    (persistent! o) => [4 3 2 1 0]

    (dosync (sort! o <))
    (persistent! o) => [0 1 2 3 4]))

^{:refer hara.ova/reverse! :added "2.1"}
(fact "reverses the order of elements in the ova"
  (let [o (ova (range 5))]
    (dosync (reverse! o))
    (persistent! o) => [4 3 2 1 0]))

^{:refer hara.ova/remove! :added "2.1"}
(fact "removes data from the ova that matches a selector"

  (let [o (ova (range 10))]
    (dosync (remove! o odd?))
    (persistent! o))
  => [0 2 4 6 8]

  (let [o (ova (range 10))]
    (dosync (remove! o '(not= 3)))
    (persistent! o))
  => [3]

  (let [o (ova (range 10))]
    (dosync (remove! o #{'(< 3) '(> 6)}))
    (persistent! o))
  => [3 4 5 6])

^{:refer hara.ova/filter! :added "2.1"}
(fact "filter is the opposite of reverse. It keeps the
  elements that matches a selector instead of throwing
  them away"

  (let [o (ova (range 10))]
    (dosync (filter! o odd?))
    (persistent! o))
  => [1 3 5 7 9]

  (let [o (ova (range 10))]
    (dosync (filter! o #{'(< 3) '(> 6)}))
    (persistent! o))
  => [0 1 2 7 8 9])

^{:refer hara.ova/clone :added "2.1"}
(fact "creates an exact copy of the ova, including its watches"
  (let [o (ova (range 10))
        _ (watch/set o {:a (fn [_ _ _ _ _])})
        o-clone (clone o)]
    (persistent! o-clone) => (range 10)
    (watch/list o-clone) => (just {:a fn?})))

^{:refer hara.ova/split :added "2.1"}
(fact "creates an exact copy of the ova, including its watches"
  (let [o (ova (range 10))
        sp (dosync (split o #{'(< 3) '(> 6)}))]
    (persistent! (sp true))  => [0 1 2 7 8 9]
    (persistent! (sp false)) => [3 4 5 6]))

^{:refer hara.ova/!! :added "2.1"}
(fact "sets the value of a given data cell in the ova"
  (dosync (-> (range 5) (ova) (!! 1 0) persistent!))
  => [0 0 2 3 4]
  (dosync (-> (range 5) (ova) (!! #{1 2} 0) persistent!))
  => [0 0 0 3 4]
  (dosync (-> (range 5) (ova) (!! even? 0) persistent!))
  => [0 1 0 3 0])

^{:refer hara.ova/<< :added "2.1"}
(fact "outputs the persistent value of an entire body after manipulation"
  (<< (def obj-a (ova [1 2 3 4 5]))
      (append! obj-a 6 7 8 9))
  => [1 2 3 4 5 6 7 8 9])

^{:refer hara.ova/!> :added "2.1"}
(fact "applies a set of transformations to a selector on the ova"
  (let [ov (ova [{:id :1}])]

    (dosync (!> ov 0
                (assoc-in [:a :b] 1)
                (update-in [:a :b] inc)
                (assoc :c 3)))
    (<< ov))
  => [{:id :1 :c 3 :a {:b 2}}])

^{:refer hara.common.watch/add :added "2.1"}
(fact "testing the watch/add function with map"
  (let [ov     (ova [1 2 3 4])
        out    (atom [])
        cj-fn  (fn  [_ _ _ p v]
                 (swap! out conj [p v]))
        _      (watch/add ov :conj cj-fn)
        _      (dosync (map! ov inc))]
    (persistent! ov) => [2 3 4 5]
    (sort @out) => [[1 2] [2 3] [3 4] [4 5]]))

^{:refer hara.common.watch/add :added "2.1"}
(fact "testing the watch/add with ova functionality"
  (let [ov     (ova [])
        out    (atom [])
        o-fn  (fn  [_ _ p v]
                (swap! out conj [p v]))
        _      (watch/add ov :conj o-fn
                          {:type :ova
                           :select #(mapv deref %)})
        _      (dosync (conj! ov 1))
        _      (dosync (conj! ov 2))
        _      (dosync (conj! ov 3))]
    (persistent! ov) => [1 2 3]
    (sort @out) => [[[] [1]] [[1] [1 2]] [[1 2] [1 2 3]]]))

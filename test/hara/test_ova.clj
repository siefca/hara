(ns hara.test-ova
  (:use midje.sweet
        hara.checkers
        hara.common)
  (:require [hara.ova :as v] :reload))

(def ^:dynamic *ova* (v/ova))

(against-background
  [(before :facts
           (dosync (v/reinit! *ova* [{:id 1 :val 1} {:id 2 :val 1}
                                     {:id 3 :val 2} {:id 4 :val 2}])))]

  (facts "indices"
    (v/indices *ova*) => (throws Exception)
    (v/indices *ova* 0) => #{0}
    (v/indices *ova* 2) => #{2}
    (v/indices *ova* #{1 2}) => #{1 2}
    (v/indices *ova* #{0}) => #{0}
    (v/indices *ova* #{4}) => #{}
    (v/indices *ova* #(odd? (:id %))) => #{0 2}
    (v/indices *ova* #(even? (:val %))) => #{2 3}
    (v/indices *ova* [:id 1]) => #{0}
    (v/indices *ova* [:val 1]) => #{0 1}
    (v/indices *ova* [:id odd?]) => #{0 2}
    (v/indices *ova* [:val even?]) => #{2 3}
    (v/indices *ova* [:val even? :id odd?]) => #{2})

  (fact "select will grab the necessary entries"
    (v/select *ova*) => (throws Exception)
    (v/select *ova* 0) => #{{:id 1 :val 1}}
    (v/select *ova* 2) => #{{:id 3 :val 2}}
    (v/select *ova* #{1 2}) => #{{:id 2 :val 1} {:id 3 :val 2}}
    (v/select *ova* #{0}) => #{{:id 1 :val 1}}
    (v/select *ova* #{4}) => #{}
    (v/select *ova* 2) => #{{:id 3 :val 2}}
    (v/select *ova* #(odd? (:id %))) => #{{:id 1 :val 1} {:id 3 :val 2}}
    (v/select *ova* #(even? (:val %))) => #{{:id 3 :val 2} {:id 4 :val 2}}
    (v/select *ova* [:id 1]) => #{{:id 1 :val 1}}
    (v/select *ova* [:val 1]) => #{{:id 1 :val 1} {:id 2 :val 1}}
    (v/select *ova* [:val nil?]) => #{}
    (v/select *ova* [:id odd?]) => #{{:id 1 :val 1} {:id 3 :val 2}}
    (v/select *ova* [:val even?]) => #{{:id 3 :val 2} {:id 4 :val 2}}
    (v/select *ova* [:val even? :id odd?]) => #{{:id 3 :val 2}}
    (v/select *ova* #{[:id 1] [:val 2]})
    => #{{:id 1 :val 1} {:id 3 :val 2} {:id 4 :val 2}}))


(against-background
  [(before :checks
           (dosync (v/reinit! *ova* [{:id 1 :val 1} {:id 2 :val 1}
                                     {:id 3 :val 2} {:id 4 :val 2}])))]

  (fact "map!"
    (dosync (v/map! *ova* dissoc :val))
    => (is-ova [{:id 1} {:id 2} {:id 3} {:id 4}])

    (dosync (v/map! *ova* assoc :val 10))
    => (is-ova [{:id 1 :val 10} {:id 2 :val 10}
                {:id 3 :val 10} {:id 4 :val 10}])

    (dosync (v/map! *ova* empty?))
    => (is-ova [false false false false]))

  (fact "map-indexed!"
    (dosync (v/map-indexed! *ova* (fn [i obj] (assoc obj :val i))))
    => (is-ova [{:id 1 :val 0} {:id 2 :val 1}
                {:id 3 :val 2} {:id 4 :val 3}]))

  (fact "smap!"
    (dosync (v/smap! *ova* [:val 1] assoc :val 100))
    => (is-ova [{:id 1 :val 100} {:id 2 :val 100}
                {:id 3 :val 2} {:id 4 :val 2}])

    (dosync (v/smap! *ova* [:id 4 :val 2] dissoc :val))
    => (is-ova [{:id 1 :val 1} {:id 2 :val 1}
                {:id 3 :val 2} {:id 4}])

    (dosync (v/smap! *ova* #{[:id 4] [:val 1]} dissoc :val))
    => (is-ova [{:id 1} {:id 2} {:id 3 :val 2} {:id 4}]))

  (fact "smap-indexed!"
    (dosync (v/smap-indexed! *ova* [:val 1] (fn [i obj] {:id i})))
    => (is-ova [{:id 0} {:id 1}
                {:id 3 :val 2} {:id 4 :val 2}])))


(against-background
  [(before :checks
           (dosync (v/reinit! *ova* (range 10))))]

 (fact "reverse!"
    (dosync (v/reverse! *ova*)) => (is-ova [9 8 7 6 5 4 3 2 1 0]))

  (fact "sort"
    (dosync (v/sort! *ova* >)) => (is-ova [9 8 7 6 5 4 3 2 1 0])
    (dosync (v/sort! *ova* <)) => (is-ova (range 10)))

  (fact "concat!"
    (dosync (v/concat! *ova* (range 10 20))) => (is-ova (range 20))
    (dosync (v/concat! *ova* *ova*)) => (is-ova (concat (range 10) (range 10))))

  (fact "append!"
    (dosync (v/append! *ova* 10 11 12)) => (is-ova (range 13)))

  (fact "insert!"
    (dosync (v/insert! *ova* 10)) => (is-ova [0 1 2 3 4 5 6 7 8 9 10])
    (dosync (v/insert! *ova* 5 5)) => (is-ova [0 1 2 3 4 5 5 6 7 8 9])
    (dosync (v/insert! *ova* :N 0)) => (is-ova [:N 0 1 2 3 4 5 6 7 8 9])
    (dosync (v/insert! *ova* :N 10)) => (is-ova [0 1 2 3 4 5 6 7 8 9 :N]))

  (fact "filter!"
    (dosync (v/filter! *ova* odd?)) => (is-ova [1 3 5 7 9])
    (dosync (v/filter! *ova* (%? not= 3))) => (is-ova [0 1 2 4 5 6 7 8 9])
    (dosync (v/filter! *ova* #{(%? < 3) (%? > 6)})) => (is-ova [0 1 2 7 8 9])
    (dosync (v/filter! *ova* [identity (%? >= 3) identity (%? <= 6)]))
    => (is-ova [ 3 4 5 6]))

  (fact "remove!"
    (dosync (v/remove! *ova* odd?)) => (is-ova [0 2 4 6 8])
    (dosync (v/remove! *ova* (%? not= 3))) => (is-ova [3])
    (dosync (v/remove! *ova* #{(%? < 3) (%? > 6)})) => (is-ova [3 4 5 6]))
)


(comment
  (facts "update will operate on maps only"
    (against-background
      (before :checks
              (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 2} 0]))))

    (dosync (v/update ov 0 {:val 2})) => (is-ova {:id 1 :val 2} {:id 2 :val 2} 0)
    (dosync (v/update ov 1 {:id 3 :val 3 :valb 4})) => (is-ova {:id 1 :val 1} {:id 3 :val 3 :valb 4} 0)
    (dosync (v/update ov 2)) => (throws Exception)
    (dosync (v/update ov [:id 1] {:val 2})) => (is-ova {:id 1 :val 2} {:id 2 :val 2} 0))

  (facts "update using array checks"
    (against-background
      (before :checks
              (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 2}]))))

    (dosync (v/update ov [:id 1] {:val 2})) => (is-ova {:id 1 :val 2} {:id 2 :val 2})
    (dosync (v/update ov [:val 2] {:val 3})) => (is-ova {:id 1 :val 1} {:id 2 :val 3}))

  (facts "update using array checks"
    (against-background
      (before :checks
              (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))))

    (dosync (v/update ov [:id 1] {:val 2})) => (is-ova {:id 1 :val 2} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
    (dosync (v/update ov [:val 2] {:val 3})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 3})
    (dosync (v/update ov #(odd? (:id %)) {:val 3})) => (is-ova {:id 1 :val 3} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 2})
    (dosync (v/update ov #(even? (:val %)) {:val 4})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 4} {:id 4 :val 4})
    (dosync (v/update ov #{1 2} {:val 4})) => (is-ova {:id 1 :val 1} {:id 2 :val 4} {:id 3 :val 4} {:id 4 :val 2})
    (dosync (v/update ov #{0} {:val 4})) => (is-ova {:id 1 :val 4} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
    (dosync (v/update ov #{4} {:val 4})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
    (dosync (v/update ov [:id odd?] {:val 3})) => (is-ova {:id 1 :val 3} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 2})
    (dosync (v/update ov [:val even?] {:val 4})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 4} {:id 4 :val 4})
    (dosync (v/update ov [:id odd? :val even?] {:valb 4})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2 :valb 4} {:id 4 :val 2}))

  (facts "set-val is like update but replaces the entire cell"
    (against-background
      (before :checks
              (def ov (v/ova [1 2 3 4 5 6]))))
    (dosync (v/set-val ov 0 0)) => (is-ova 0 2 3 4 5 6)
    (dosync (v/set-val ov #{1 2} 0)) => (is-ova 1 0 0 4 5 6)
    (dosync (v/set-val ov odd? 0)) => (is-ova 0 2 0 4 0 6)
    (dosync (v/set-val ov [:id 1] 0)) => (is-ova 1 2 3 4 5 6)
    (dosync (v/set-val (v/ova [{:id 1 :val 1} {:id 2 :val 1}]) [:id 1] 0)) => (is-ova 0 {:id 2 :val 1}))

  (facts "update-in"
    (against-background
      (before :checks
              (def ov (v/ova [{:id 1 :val {:a 1}}]))))
    (dosync (v/update-in ov [:id 1] [:val :x] (constantly 2))) => (is-ova {:id 1 :val {:a 1 :x 2}})
    (dosync (v/update-in ov [:id 1] [:val :a] (constantly 2))) => (is-ova {:id 1 :val {:a 2}}))

  (facts "set-in"
    (against-background
      (before :checks
              (def ov (v/ova [{:id 1 :val {:a 1}}]))))
    (dosync (v/set-in ov [:id 1] [:val :x] 2)) => (is-ova {:id 1 :val {:a 1 :x 2}})
    (dosync (v/set-in ov [:id 1] [:val :a] 2)) => (is-ova {:id 1 :val {:a 2}}))

  (facts "delete using array checks"
    (against-background
      (before :checks
              (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))))

    (dosync (v/delete ov [:id 1])) => (is-ova {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
    (dosync (v/delete ov [:val 2])) => (is-ova {:id 1 :val 1} {:id 2 :val 1})
    (dosync (v/delete ov #(odd? (:id %)))) => (is-ova {:id 2 :val 1} {:id 4 :val 2})
    (dosync (v/delete ov #(even? (:val %)))) => (is-ova {:id 1 :val 1} {:id 2 :val 1})
    (dosync (v/delete ov #{1 2})) => (is-ova {:id 1 :val 1} {:id 4 :val 2})
    (dosync (v/delete ov #{0})) => (is-ova {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
    (dosync (v/delete ov #{4})) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2})
    (dosync (v/delete ov [:id odd?])) => (is-ova  {:id 2 :val 1} {:id 4 :val 2})
    (dosync (v/delete ov [:val even?])) => (is-ova {:id 1 :val 1} {:id 2 :val 1})
    (dosync (v/delete ov [:id odd? :val even?])) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 4 :val 2}))

  (facts "insert puts a new object"
    (against-background
      (before :checks
              (def ov (v/ova [{:id 1 :val 1}]))))
    (dosync (v/insert ov {:id 2 :val 1})) => (is-ova {:id 1 :val 1} {:id 2 :val 1})
    (dosync (v/insert ov {:id 2 :val 1} 0)) => (is-ova {:id 2 :val 1} {:id 1 :val 1})
    (dosync (v/insert ov {:id 2 :val 1} 1)) => (is-ova {:id 1 :val 1} {:id 2 :val 1})
    (dosync (v/insert ov {:id 2 :val 1} -1)) => (throws Exception)
    (dosync (v/insert ov {:id 2 :val 1} 2)) => (throws Exception)
    (dosync (-> ov
                (v/insert {:id 3 :val 1} 1)
                (v/insert {:id 2 :val 1} 1))) => (is-ova {:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 1}))

  (fact "reverse"
    (dosync (v/reverse (v/ova [1 2 3 4 5]))) => (is-ova 5 4 3 2 1))

  (fact "filter"
    (dosync (v/filter (v/ova [1 2 3 4 5 6 7]) odd?)) => (is-ova 1 3 5 7))

  (fact "sort"
    (dosync (v/sort (v/ova [3 2 1 5 4 7 6]))) => (is-ova 1 2 3 4 5 6 7))



  ;; Watch functionality
  (facts "testing the add-elem-watch function with map"
    (let [ov     (v/ova [1 2 3 4])
          out    (atom [])
          cj-fn  (fn  [o r k p v & args]
                   ;;(println o r k p v args )
                   (swap! out conj [p v]))
          _      (v/add-elem-watch ov :conj cj-fn)
          _      (dosync (v/map ov inc))]
      (facts "out is updated"
        ov => (is-ova 2 3 4 5)
        (sort @out) => [[1 2] [2 3] [3 4] [4 5]])))

  (facts "testing the add-elem-watch function when an element has been deleted"
    (let [ov     (v/ova [1 2 3 4])
          out    (atom [])
          cj-fn  (fn  [_ _ _ p v & args]
                   (swap! out conj [p v]))
          _      (v/add-elem-watch ov :conj cj-fn)
          _      (dosync (v/delete ov 0))
          _      (dosync (v/map ov inc))]
      (facts "out is updated"
        ov => (is-ova 3 4 5)
        (sort @out) => [[2 3] [3 4] [4 5]])))

  (facts "testing the add-elem-watch function when an element has been added"
    (let [ov     (v/ova [1 2 3 4])
          out    (atom [])
          cj-fn  (fn  [_ _ _ p v & args]
                   (swap! out conj [p v]))
          _      (v/add-elem-watch ov :conj cj-fn)
          _      (dosync (v/insert ov 3))
          _      (dosync (v/map ov inc))]
      (facts "out is updated"
        ov => (is-ova 2 3 4 5 4))))

  (facts "testing the add-elem-watch function when an elements are arbitrarily "
    (let [ov     (v/ova [1 2 3 4])
          out    (atom [])
          cj-fn  (fn  [_ _ _ p v & args]
                   (swap! out conj [p v]))
          _      (v/add-elem-watch ov :conj cj-fn)]

      (dosync (v/insert ov 1 3))
      (fact ov => (is-ova 1 2 3 1 4))

      (dosync (v/delete ov odd?))
      (fact ov => (is-ova 2 4))

      (dosync (v/map ov inc))
      (fact ov => (is-ova 3 5))

      (dosync (v/concat ov [1 2 3 4 5]))
      (fact ov  => (is-ova 3 5 1 2 3 4 5))

      (dosync (v/sort ov))
      (fact ov  => (is-ova 1 2 3 3 4 5 5))

      ;; [3 5 1 2 3 4 5]
      ;;
      ;;(dosync (fact  => (is-ova 1 2 3 3 4 5 5)))
      ;;(dosync (fact (v/map-indexed ov (fn [i x] x)) => (is-ova 1 2 3 3 4 5 5)))
      ;;(fact (dosync (v/map-indexed ov (fn [i x] i))) => (is-ova 0 1 2 3 4 5 6))

      ))
  (comment
    (def a (v/ova [5 4 3 2 6]))
    (println a)
    (dosync (v/sort a)))
  ;;(def a (v/ova [1 1 2 3 4 5]))
  ;;(v/map-indexed a (fn [i x] i))
  ;;(count a)
)

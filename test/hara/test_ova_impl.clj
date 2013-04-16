(ns hara.test-ova-impl
  (:use midje.sweet
        hara.checkers
        hara.common)
  (:refer-clojure :exclude [send])
  (:require [hara.ova.impl :as i])
  (:import hara.ova.Ova))

(def ^:dynamic *ova*)

(against-background
  [(before :facts (dosync (i/-empty *ova*)))]

  (fact "Testing the ova constructor"
    *ova* => (is-ova)
    (.seq *ova*) => nil
    (.count *ova*) => 0
    (i/sel *ova*) => (is-ref [])
    (i/-deref *ova*) => []
    (i/-persistent *ova*) => []
    (empty? *ova*) => true))

(against-background
  [(before :facts (dosync (i/-empty *ova*)
                          (i/-conj *ova* {:id :1 :val 1})
                          (i/-conj *ova* {:id :0 :val 0})))]
  (fact "basics"
    *ova* => (is-ova [{:id :1 :val 1} {:id :0 :val 0}])
    (i/sel *ova*) => (is-ref (just [(is-ref {:id :1 :val 1})
                                    (is-ref {:id :0 :val 0})]))
    (i/-deref *ova*) => (just [(is-ref {:id :1 :val 1})
                               (is-ref {:id :0 :val 0})])
    (i/-persistent *ova*) => [{:val 1, :id :1} {:val 0, :id :0}]
    (i/-seq *ova*) => [{:id :1 :val 1} {:id :0 :val 0}]
    (i/-count *ova*) => 2)

  (fact "nth"
    (i/-nth *ova* 0) => {:id :1 :val 1}
    (i/-nth *ova* -1) => (throws Exception)
    (i/-nth *ova* 2) => (throws Exception)
    (i/-nth *ova* :0) => (throws Exception))

  (fact "valAt"
    (i/-valAt *ova* 0) => {:id :1 :val 1}
    (i/-valAt *ova* :0) => {:id :0 :val 0}
    (i/-valAt *ova* :NA) => nil
    (i/-valAt *ova* :NA :NA) => :NA
    (i/-valAt *ova* :0 :id) => {:id :0 :val 0}
    (i/-valAt *ova* 0 :val nil) => {:id :0 :val 0})

  (fact "invoke"
    (i/-invoke *ova* 0 :val nil)  => {:id :0 :val 0})

  (fact "toString"
    (i/-toString *ova*) => "{:val 1, :id :1}\n{:val 0, :id :0}"))

(against-background
  [(before :facts (dosync (i/-reset *ova*)
                          (i/-addWatch *ova* :a (fn [& _]))))]
  (fact "getWatches"
    (i/-getWatches *ova*)
    => (just {:a fn?}))

  (fact "addWatch"
    (i/-addWatch *ova* :b (fn [& _]))
    (i/-getWatches *ova*)
    => (just {:a fn? :b fn?}))

  (fact "removeWatch"
    (i/-removeWatch *ova* :a)
    (i/-getWatches *ova*)
    => {}))

(against-background
  [(before :facts (do (i/-clearElemWatches *ova*)
                      (i/-addElemWatch *ova* :a (fn [& _]))))]

  (fact "getElemWatches"
    (i/-getElemWatches *ova*)
    => (just {:a fn?}))

  (fact "addElemWatch"
    (i/-addElemWatch *ova* :b (fn [& _]))
    (i/-getElemWatches *ova*)
    => (just {:a fn? :b fn?}))

  (fact "removeElemWatch"
    (i/-removeElemWatch *ova* :a)
    (i/-getElemWatches *ova*)
    => {}))

(against-background
  [(before :checks (dosync (i/-empty *ova*)
                           (i/-conj *ova* {:id :1 :val 1})
                           (i/-conj *ova* {:id :2 :val 2})))]

  (fact "empty"
    (dosync (i/-empty *ova*))
    => (is-ova [])

    (dosync (i/-pop *ova*))
    => (is-ova [{:id :1 :val 1}])

    (dosync (i/-pop (i/-pop *ova*)))
    => (is-ova [])

    (dosync (i/-assoc *ova* 0 {:id :0 :val 0}))
    => (is-ova [{:id :0 :val 0} {:id :2 :val 2}])

    (dosync (i/-assocN *ova* 2 {:id :3 :val 3}))
    => (is-ova [{:id :1 :val 1} {:id :2 :val 2} {:id :3 :val 3}])

    (dosync (i/-assoc *ova* :NON-INT {:id :0 :val 0}))
    => (throws Exception)))


(against-background
  [(before :facts (dosync (i/-reset *ova*)))]

  (facts "add-watch"
    (let [out    (atom nil)]

      (follow *ova* out :a (fn [v] (deref* v)))

      (dosync (conj! *ova* {:id 1 :val 1}))
      (persistent! *ova*) => @out => [{:id 1 :val 1}]

      (dosync (conj! *ova* {:id 2 :val 2}))
      (persistent! *ova*) => @out => [{:id 1 :val 1} {:id 2 :val 2}]

      (unfollow *ova* out :a)

      (dosync (conj! *ova* {:id 3 :val 3}))
      (persistent! *ova*) => [{:id 1 :val 1} {:id 2 :val 2} {:id 3 :val 3}]
      @out => [{:id 1 :val 1} {:id 2 :val 2}])))


(against-background
  [(before :facts (dosync (i/-reset *ova*)))]

  (facts "add-elem-watches"
    (let [*ova*      (Ova.)
          out    (atom nil)
          out-fn (fn [_ _ _ _ v & _] (reset! out (deref* v)))
          _      (.addElemWatch *ova* :elm out-fn)]

      (dosync (conj! *ova* {:id 1 :val 1}))
      (persistent! *ova*) => [{:id 1 :val 1}]
      @out => nil

      (dosync (ref-set (@*ova* 0) {:id 1 :val 2}))
      (persistent! *ova*) => [{:id 1 :val 2}]
      @out => {:id 1 :val 2}

      (dosync (alter (.valAt @*ova* 0) assoc :more 3))
      (persistent! *ova*) => [{:id 1 :val 2 :more 3}]
      @out => {:id 1 :val 2 :more 3}

      ;; remove element watch
      (do (.removeElemWatch *ova* :elm)
          (dosync (alter (.valAt @*ova* 0) assoc :more 4)))
      (persistent! *ova*) => [{:id 1 :val 2 :more 4}]
      @out {:id 1 :val 2 :more 3})))


(fact "element watches do not propagate when the ova is not
       contained anymore"
  (let [a         (dosync (conj! (Ova.) 1))
        elem-out  (atom nil)
        elem-fn   (fn [_ _ _ _ v & args]
                    (reset! elem-out v))
        norm-out  (atom nil)
        norm-fn   (fn [_ _ _ v & args]
                    (reset! norm-out v))
        _         (.addElemWatch a :elem elem-fn)
        _         (add-watch (@a 0) :norm norm-fn)
        evm       (@a 0)]

    (dosync (ref-set evm 2))
    (fact "both states should trigger"
      a => (is-ova [2])
      elem-out => (is-atom 2)
      norm-out => (is-atom 2))

    (dosync
     (pop! a)
     (ref-set evm 3))
    (fact "only the norm-out should change when element is removed"
      a => (is-ova)
      elem-out => (is-atom 2)
      norm-out => (is-atom 3))))

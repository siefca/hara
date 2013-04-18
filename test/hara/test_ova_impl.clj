(ns hara.test-ova-impl
  (:use midje.sweet
        hara.checkers
        hara.common
        hara.ova))

(def ^:dynamic *ova* (ova))

((is-ova) (ova))
(against-background
  [(before :facts (dosync (empty! *ova*)))]

  (fact "Testing the ova constructor"
    *ova* => (is-ova)
    (.seq *ova*) => nil
    (.count *ova*) => 0
    (.deref *ova*) => []
    (.persistent *ova*) => []
    (empty? *ova*) => true
    (get-ref *ova*) => (is-ref [])))

(against-background
  [(before :facts (dosync (empty! *ova*)
                          (.conj *ova* {:id :1 :val 1})
                          (.conj *ova* {:id :0 :val 0})))]
  (fact "basics"
    *ova* => (is-ova [{:id :1 :val 1} {:id :0 :val 0}])
    (get-ref *ova*) => (is-ref (just [(is-ref {:id :1 :val 1})
                                    (is-ref {:id :0 :val 0})]))
    (.deref *ova*) => (just [(is-ref {:id :1 :val 1})
                               (is-ref {:id :0 :val 0})])
    (.persistent *ova*) => [{:val 1, :id :1} {:val 0, :id :0}]
    (.seq *ova*) => [{:id :1 :val 1} {:id :0 :val 0}]
    (.count *ova*) => 2)

  (fact "nth"
    (.nth *ova* 0) => {:id :1 :val 1}
    (.nth *ova* -1) => (throws Exception)
    (.nth *ova* 2) => (throws Exception)
    (.nth *ova* :0) => (throws Exception))

  (fact "valAt"
    (.valAt *ova* 0) => {:id :1 :val 1}
    (.valAt *ova* :0) => {:id :0 :val 0}
    (.valAt *ova* :NA) => nil
    (.valAt *ova* :NA :NA) => :NA
    (.valAt *ova* :0 :id) => {:id :0 :val 0}
    (get-filtered *ova* 0 :val nil) => {:id :0 :val 0})

  (fact "invoke"
    (.invoke *ova* 0 :val nil)  => {:id :0 :val 0})

  (fact "toString"
    (.toString *ova*)"[{:val 1, :id :1} {:val 0, :id :0}]"))

(against-background
  [(before :facts (dosync (reinit! *ova*)
                          (.addWatch *ova* :a (fn [& _]))))]
  (fact "getWatches"
    (.getWatches *ova*)
    => (just {:a fn?}))

  (fact "addWatch"
    (.addWatch *ova* :b (fn [& _]))
    (.getWatches *ova*)
    => (just {:a fn? :b fn?}))

  (fact "removeWatch"
    (.removeWatch *ova* :a)
    (.getWatches *ova*)
    => {}))

(against-background
  [(before :facts (do (clear-elem-watches *ova*)
                      (add-elem-watch *ova* :a (fn [& _]))))]

  (fact "get-elem-watches"
    (get-elem-watches *ova*)
    => (just {:a fn?}))

  (fact "add-elem-watch"
    (add-elem-watch *ova* :b (fn [& _]))
    (get-elem-watches *ova*)
    => (just {:a fn? :b fn?}))

  (fact "remove-elem-watch"
    (remove-elem-watch *ova* :a)
    (get-elem-watches *ova*)
    => {}))

(against-background
  [(before :checks (dosync (empty! *ova*)
                           (.conj *ova* {:id :1 :val 1})
                           (.conj *ova* {:id :2 :val 2})))]

  (fact "empty"
    (dosync (empty! *ova*))
    => (is-ova [])

    (dosync (.pop *ova*))
    => (is-ova [{:id :1 :val 1}])

    (dosync (.pop (.pop *ova*)))
    => (is-ova [])

    (dosync (.assoc *ova* 0 {:id :0 :val 0}))
    => (is-ova [{:id :0 :val 0} {:id :2 :val 2}])

    (dosync (.assoc *ova* 2 {:id :3 :val 3}))
    => (is-ova [{:id :1 :val 1} {:id :2 :val 2} {:id :3 :val 3}])

    (dosync (.assoc *ova* :NON-INT {:id :0 :val 0}))
    => (throws Exception)))

(against-background
  [(before :facts (dosync (reinit! *ova*)))]

  (facts "add-watch"
    (let [out    (atom nil)]

      (latch *ova* out (fn [v] (deref* v)))

      (dosync (conj! *ova* {:id 1 :val 1}))
      (persistent! *ova*) => @out => [{:id 1 :val 1}]

      (dosync (conj! *ova* {:id 2 :val 2}))
      (persistent! *ova*) => @out => [{:id 1 :val 1} {:id 2 :val 2}]

      (unlatch *ova* out)

      (dosync (conj! *ova* {:id 3 :val 3}))
      (persistent! *ova*) => [{:id 1 :val 1} {:id 2 :val 2} {:id 3 :val 3}]
      @out => [{:id 1 :val 1} {:id 2 :val 2}])))


(against-background
  [(before :facts (dosync (reinit! *ova*)))]

  (facts "add-elem-watches"
    (let [*ova*      (ova)
          out    (atom nil)
          out-fn (fn [_ _ _ _ v & _] (reset! out (deref* v)))
          _      (add-elem-watch *ova* :elm out-fn)]

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
      (do (remove-elem-watch *ova* :elm)
          (dosync (alter (.valAt @*ova* 0) assoc :more 4)))
      (persistent! *ova*) => [{:id 1 :val 2 :more 4}]
      @out {:id 1 :val 2 :more 3})))


(fact "element watches do not propagate when the ova is not
       contained anymore"
  (let [a         (dosync (conj! (ova) 1))
        elem-out  (atom nil)
        elem-fn   (fn [_ _ _ _ v & args]
                    (reset! elem-out v))
        norm-out  (atom nil)
        norm-fn   (fn [_ _ _ v & args]
                    (reset! norm-out v))
        _         (add-elem-watch a :elem elem-fn)
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

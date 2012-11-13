(ns hara.test-eva-aot
  (:refer-clojure :exclude [swap! reset!])
  (:use midje.sweet
        [hara.fn :only [deref*]]
        [hara.data.evom :only [evom swap! reset! add-watches remove-watches]])
  (:import hara.data.Eva))

(defn is-atom [& [value]]
  (fn [at]
    (if (and (instance? clojure.lang.Atom at)
             (= @at value))
      true)))

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

(fact "testing the DynaRec constructor"
  (Eva.) => (is-eva))


(fact "simple specification"
  (let [ev (Eva.)]
    (conj! ev {:id :0})
    (facts "seq"
      ev => (is-eva {:id :0})
      (first @ev) => (is-evom {:id :0})
      (count ev) => 1
      (first (.seq ev))   => {:id :0}
      (first (seq ev))    => {:id :0})
    (facts "valAt"
      (.valAt ev 0)       => {:id :0}
      (.valAt ev 0 :na)   => {:id :0}
      (.valAt ev 1 :na)    => :na)
    (facts "nth"
      (.nth ev 0)         => {:id :0}
      (.nth ev 0 :na)     => {:id :0}
      (.nth ev 1)          => nil
      (.nth ev 1 :na)      => :na
      (nth ev 0)          => {:id :0}
      (nth ev 0 :na)      => {:id :0}
      (nth ev 1)           => nil
      (nth ev 1 :na)       => :na)
    (facts "persistent"
      (.persistent ev)     => [{:id :0}])
      (persistent! ev)     => [{:id :0}]))

(fact "testing append and pop operations"
  (against-background
    (before :checks (def a (Eva.))))
  (count a) => 0
  (-> a (conj! 1)) => (is-eva 1)
  (-> a (conj! 0) (conj! 1)) => (is-eva 0 1)
  (-> a (assoc! 0 1) (assoc! 1 1)) => (is-eva 1 1)
  (-> a (conj! 0) (conj! 1) pop! pop!) => (is-eva)
  (-> a pop!) => (throws Exception))



(facts "add-watch"
  (let [a      (Eva.)
        out    (evom nil)
        out-fn (fn [_ _ _ v & _] (reset! out (deref* v)))
        _      (add-watch a :a out-fn)]

    (fact "a is initialised"
      a   => (is-eva)
      out => (is-evom))

    (dosync (conj! a {:id 1 :contents 1}))
    (fact "conj! changes both a and out"
      (persistent! a) => [{:id 1 :contents 1}]
      (a 0) => {:id 1 :contents 1}
      out => (is-evom [{:id 1 :contents 1}]))

    (dosync (conj! a {:id 2 :contents 2}))
    (fact "conj! changes both a and out"
      (persistent! a) => [{:id 1 :contents 1}
                          {:id 2 :contents 2}]
      (a 0) => {:id 1 :contents 1}
      (a 1) => {:id 2 :contents 2}
      out => (is-evom [{:id 1 :contents 1}
                       {:id 2 :contents 2}]))

    (remove-watch a :a) ;; remove watch, <out> will not update when a is modified
    (dosync (conj! a {:id 3 :contents 3}))
    (fact "conj! changes both a and out"
      (persistent! a) => [{:id 1 :contents 1}
                          {:id 2 :contents 2}
                          {:id 3 :contents 3}]
      out => (is-evom [{:id 1 :contents 1}

                       {:id 2 :contents 2}]))))

(comment
(def a (Eva.))
(.addElemWatch a :print println)
(.addElemWatch a :print1 println)
(.getWatches (@a 0))
(reset! (@a 0) 4)
)


(facts "add-elem-watches"
  (let [a      (Eva.)
        out    (evom nil)
        out-fn (fn [_ _ _ _ v & _] (reset! out (deref* v)))
        _      (.addElemWatch a :a out-fn)]

    (dosync (conj! a {:id 1 :contents 1}))
    (fact "conj! does not affect out"
      (persistent! a) => [{:id 1 :contents 1}]
      (a 0) => {:id 1 :contents 1}
      out => (is-evom)
      )

    (do (reset! (@a 0) {:id 1 :contents 2}))
    (fact "reset! methods"
      (persistent! a) => [{:id 1 :contents 2}]
      (a 0) => {:id 1 :contents 2}
      out => (is-evom {:id 1 :contents 2}))

    (swap! (.valAt @a 0) assoc :a 1)
    (fact "swap! methods"
      (persistent! a) => [{:id 1 :contents 2 :a 1}]
      (a 0) => {:id 1 :contents 2 :a 1}
      out => (is-evom (a 0))
      (:a (.getElemWatches a)) =not=> nil)

    ;; remove element watch
    (do (.removeElemWatch a :a)
        (swap! (.valAt @a 0) assoc :a 2))
    (fact "remove watch"
      (deref* a) => [{:id 1 :contents 2 :a 2}]
      (a 0) => {:id 1 :contents 2 :a 2}
      out =not=> (is-evom (a 0))
      (:a (.getElemWatches a)) => nil)))


(fact "tests that watches do not propagate when there are no
       element watch functions"
  (let [a         (conj! (Eva.) 1)
        elem-out  (atom nil)
        elem-fn   (fn [_ _ _ _ v & args]
                    (reset! elem-out v))
        norm-out  (atom nil)
        norm-fn   (fn [_ _ _ v & args]
                    (reset! norm-out v))
        _         (.addElemWatch a :elem elem-fn)
        _         (add-watch (@a 0) :norm norm-fn)
        evm       (@a 0)]
    (fact "initial elements are nil"
      elem-out => (is-atom nil)
      norm-out => (is-atom nil))

    (reset! evm 2)
    (fact "both states should trigger"
      a => (is-eva 2)
      elem-out => (is-atom 2)
      norm-out => (is-atom 2))

    (.removeElemWatch a :elem)
    (reset! evm 3)
    (fact "only the norm-out should change when watch is removed"
      a => (is-eva 3)
      elem-out => (is-atom 2)
      norm-out => (is-atom 3))))


(fact "tests that watches do not propagate when the evom is not
       contained anymore"
  (let [a         (conj! (Eva.) 1)
        elem-out  (atom nil)
        elem-fn   (fn [_ _ _ _ v & args]
                    (reset! elem-out v))
        norm-out  (atom nil)
        norm-fn   (fn [_ _ _ v & args]
                    (reset! norm-out v))
        _         (.addElemWatch a :elem elem-fn)
        _         (add-watch (@a 0) :norm norm-fn)
        evm       (@a 0)]
    (fact "initial elements are nil"
      a => (is-eva 1)
      elem-out => (is-atom nil)
      norm-out => (is-atom nil))

    (reset! evm 2)
    (fact "both states should trigger"
      a => (is-eva 2)
      elem-out => (is-atom 2)
      norm-out => (is-atom 2))

    (pop! a)
    (reset! evm 3)
    (fact "only the norm-out should change when element is removed"
      a => (is-eva)
      elem-out => (is-atom 2)
      norm-out => (is-atom 3))))

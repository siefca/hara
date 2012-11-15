(ns hara.test-ova-aot
  (:use midje.sweet
        [hara.fn :only [deref*]])
  (:import hara.data.Ova))

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

(fact "testing the DynaRec constructor"
  (Ova.) => (is-ova))


(fact "simple specification"
  (let [ev (Ova.)]
    (dosync (conj! ev {:id :0}))
    (facts "seq"
      ev => (is-ova {:id :0})
      (first @ev) => (is-ref {:id :0})
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
    (before :checks (def a (Ova.))))
  (count a) => 0
  (dosync (-> a (conj! 1))) => (is-ova 1)
  (dosync (-> a (conj! 0) (conj! 1))) => (is-ova 0 1)
  (dosync (-> a (assoc! 0 1) (assoc! 1 1))) => (is-ova 1 1)
  (dosync (-> a (conj! 0) (conj! 1) pop! pop!)) => (is-ova)
  (dosync (-> a pop!)) => (throws Exception))



(facts "add-watch"
  (let [a      (Ova.)
        out    (atom nil)
        out-fn (fn [_ _ _ v] (reset! out (deref* v)))
        _      (add-watch a :a out-fn)]

    (fact "a is initialised"
      a   => (is-ova)
      out => (is-atom))

    (dosync (conj! a {:id 1 :contents 1}))
    (fact "conj! changes both a and out"
      (persistent! a) => [{:id 1 :contents 1}]
      (a 0) => {:id 1 :contents 1}
      out => (is-atom [{:id 1 :contents 1}]))

    (dosync (conj! a {:id 2 :contents 2}))
    (fact "conj! changes both a and out"
      (persistent! a) => [{:id 1 :contents 1}
                          {:id 2 :contents 2}]
      (a 0) => {:id 1 :contents 1}
      (a 1) => {:id 2 :contents 2}
      out => (is-atom [{:id 1 :contents 1}
                       {:id 2 :contents 2}]))

    (remove-watch a :a) ;; remove watch, <out> will not update when a is modified
    (dosync (conj! a {:id 3 :contents 3}))
    (fact "conj! changes both a and out"
      (persistent! a) => [{:id 1 :contents 1}
                          {:id 2 :contents 2}
                          {:id 3 :contents 3}]
      out => (is-atom [{:id 1 :contents 1}

                       {:id 2 :contents 2}]))))

(comment
(def a (Ova.))
(.addElemWatch a :print println)
(.addElemWatch a :print1 println)
(.getWatches (@a 0))
(reset! (@a 0) 4)
)


(facts "add-elem-watches"
  (let [a      (Ova.)
        out    (atom nil)
        out-fn (fn [_ _ _ _ v & _] (reset! out (deref* v)))
        _      (.addElemWatch a :a out-fn)]

    (dosync (conj! a {:id 1 :contents 1}))
    (fact "conj! does not affect out"
      (persistent! a) => [{:id 1 :contents 1}]
      (a 0) => {:id 1 :contents 1}
      out => (is-atom)
      )

    (dosync (ref-set (@a 0) {:id 1 :contents 2}))
    (fact "reset! methods"
      (persistent! a) => [{:id 1 :contents 2}]
      (a 0) => {:id 1 :contents 2}
      out => (is-atom {:id 1 :contents 2}))

    (dosync (alter (.valAt @a 0) assoc :a 1))
    (fact "alter methods"
      (persistent! a) => [{:id 1 :contents 2 :a 1}]
      (a 0) => {:id 1 :contents 2 :a 1}
      out => (is-atom (a 0))
      (:a (.getElemWatches a)) =not=> nil)

    ;; remove element watch
    (do (.removeElemWatch a :a)
        (dosync (alter (.valAt @a 0) assoc :a 2)))
    (fact "remove watch"
      (deref* a) => [{:id 1 :contents 2 :a 2}]
      (a 0) => {:id 1 :contents 2 :a 2}
      out =not=> (is-atom (a 0))
      (:a (.getElemWatches a)) => nil)))


(fact "tests that watches do not propagate when there are no
       element watch functions"
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
    (fact "initial elements are nil"
      elem-out => (is-atom nil)
      norm-out => (is-atom nil))

    (dosync (ref-set evm 2))
    (fact "both states should trigger"
      a => (is-ova 2)
      elem-out => (is-atom 2)
      norm-out => (is-atom 2))

    (.removeElemWatch a :elem)
    (dosync (ref-set evm 3))
    (fact "only the norm-out should change when watch is removed"
      a => (is-ova 3)
      elem-out => (is-atom 2)
      norm-out => (is-atom 3))))


(fact "tests that watches do not propagate when the evom is not
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
    (fact "initial elements are nil"
      a => (is-ova 1)
      elem-out => (is-atom nil)
      norm-out => (is-atom nil))

    (dosync (ref-set evm 2))
    (fact "both states should trigger"
      a => (is-ova 2)
      elem-out => (is-atom 2)
      norm-out => (is-atom 2))

    (dosync
     (pop! a)
     (ref-set evm 3))
    (fact "only the norm-out should change when element is removed"
      a => (is-ova)
      elem-out => (is-atom 2)
      norm-out => (is-atom 3))))

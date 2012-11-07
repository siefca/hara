(ns hara.test-dyna-rec
  (:use midje.sweet
        [hara.fn :only [deref*]]
        [hara.iotam :only [iswap! ireset!]])
  (:import hara.data.DynaRec))

(fact "testing the DynaRec constructor"
  @(DynaRec.) => {})

(fact "getRequired returns a set of required keys "
  (.getRequired (DynaRec.)) => #{:id}
  (.getRequired (DynaRec. [:id :contents :k1 :k2]))
  => #{:id :contents :k1 :k2})

(fact "setRequired sets the keys"
  (let [a (DynaRec.)]
    (.setRequired a [:contents])
    (.getRequired a))
  => #{:id :contents})

(fact "when an entry is added without one of the required keys, it fails"
  (let [a (DynaRec. [:contents])]
    (dosync (conj! a {:id 1})))
  => (throws AssertionError))

(facts "an entry is added with all the required keys, it succeeds"
  (let [a (DynaRec.)
        _ (dosync (conj! a {:id 1}))]
    (persistent! a))
  => {1 {:id 1}}

  (tabular
   "more use cases and examples"
   (fact (let [a (DynaRec.)
               _ (dosync (conj! a ?entry))]
           (persistent! a))
     => ?expected)
   ?entry                ?expected
   {:id 1}               {1 {:id 1}}
   {:id 2}               {2 {:id 2}}
   {:id 1 :contents 1}   {1 {:id 1 :contents 1}}
   {:id 1 :contents 2}   {1 {:id 1 :contents 2}}))

(facts
  (let [a (DynaRec. [:contents])]
    (dosync (conj! a {:id 1 :contents 1}))
    (persistent! a))
  => {1 {:id 1 :contents 1}})

(fact "an entry can only have one entry with the id"
  (let [a (DynaRec. [:contents])]
    (dosync
     (assoc! a 1 {:id 1 :contents 1})
     (persistent! (assoc! a 1 {:id 1 :contents 2}))))
  => {1 {:id 1 :contents 2}})

(facts "uses of the dynarec"
  (let [a (DynaRec. [:contents])]
    (dosync
     (assoc! a 1 {:id 1 :contents 1})
     (assoc! a 2 {:id 2 :contents 2})
     (assoc! a 3 {:id 3 :contents 3}))

    (fact "counting"
      (count a) => 3)

    (fact "access and invoking"
      @(a 3) => {:id 3 :contents 3})

    (fact "seq"
      (deref* (sort (seq a)))
      => [[1 {:id 1 :contents 1}]
          [2 {:id 2 :contents 2}]
          [3 {:id 3 :contents 3}]])

    (fact "dissoc"
      (persistent!
       (dosync (dissoc! a 3))) =>
       {1 {:id 1 :contents 1} 2 {:id 2 :contents 2}} )))

(facts "add-watch example of dynarec"
  (let [a      (DynaRec.)
        out    (atom nil)
        out-fn (fn [_ _ _ v & _] (reset! out (deref* v)))
        _      (add-watch a :a out-fn)]

    (fact "a is initialised"
      @a   => {}
      @out => nil)

    (dosync (conj! a {:id 1 :contents 1}))
    (fact "conj! changes both a and out"
      (deref* a) => {1 {:id 1 :contents 1}}
      @(a 1) => {:id 1 :contents 1}
      @out => {1 {:id 1 :contents 1}})

    (dosync (conj! a {:id 2 :contents 2}))
    (fact "conj! changes both a and out"
      (deref* a) => {1 {:id 1 :contents 1}
                     2 {:id 2 :contents 2}}
      @(a 1) => {:id 1 :contents 1}
      @(a 2) => {:id 2 :contents 2}
      @out => {1 {:id 1 :contents 1}
               2 {:id 2 :contents 2}})

    (remove-watch a :a) ;; remove watch, <out> will not update when a is modified
    (dosync (conj! a {:id 3 :contents 3}))
    (fact "conj! changes both a and out"
      (deref* a) => {1 {:id 1 :contents 1}
                     2 {:id 2 :contents 2}
                     3 {:id 3 :contents 3}}
      @out => {1 {:id 1 :contents 1}
               2 {:id 2 :contents 2}})))

(facts "add-watches example of dynarec"
  (let [a      (DynaRec.)
        out    (atom nil)
        out-fn (fn [_ _ _ v & _] (reset! out (deref* v)))
        _      (.addElemWatch a :a out-fn)]

    (dosync (conj! a {:id 1 :contents 1}))
    (fact "conj! changes both a and out"
      (deref* a) => {1 {:id 1 :contents 1}}
      @(a 1) => {:id 1 :contents 1}
      @out => nil)

    (do (ireset! (a 1) {:id 1 :contents 2}))
    (fact "conj! changes both a and out"
      (deref* a) => {1 {:id 1 :contents 2}}
      @(a 1) => {:id 1 :contents 2}
      @out => {:id 1 :contents 2})

    (iswap! (.valAt a 1) assoc  :a 1)
    (fact "conj! changes both a and out"
      (deref* a) => {1 {:id 1 :contents 2 :a 1}}
      @(a 1) => {:id 1 :contents 2 :a 1}
      @out => @(a 1)
      (:a (.getElemWatches a)) =not=> nil)

    ;; remove element watch
    (do (.removeElemWatch a :a)
        (iswap! (.valAt a 1) assoc :a 2))
    (fact "conj! changes both a and out"
      (deref* a) => {1 {:id 1 :contents 2 :a 2}}
      @(a 1) => {:id 1 :contents 2 :a 2}
      @out =not=> @(a 1)
      (:a (.getElemWatches a)) => nil)))

(ns hara.test-dyna-rec
  (:use midje.sweet)
  (:import hara.data.DynaRec))

(fact "testing the DynaRec constructor"
  @(DynaRec.) => {})

(fact "getRequired returns a set of required keys "
  (.getRequired (DynaRec.)) => #{:id}
  (.getRequired (DynaRec. [:id :contents :k1 :k2]))
  => #{:id :contents :k1 :k2})

(fact "setRequired sets the keys"
  (let [a (DynaRec.)]
    (.setRequired a [ :contents])
    (.getRequired a))
  => #{:id :contents})

(fact "when an entry is added without one of the required keys, it fails"
  (let [a (DynaRec. [:contents])]
    (dosync (conj! a {:id 1})))
  => (throws AssertionError))

(fact "an entry is added with all the required keys, it succeeds"
  (let [a (DynaRec.)]
    (dosync (conj! a {:id 1}))
    (persistent! a))
  => {1 {:id 1}}

  (let [a (DynaRec.)]
    (dosync (conj! a {:id 1 :contents 1}))
    (persistent! a))
  => {1 {:id 1 :contents 1}}

  (let [a (DynaRec. [:contents])]
    (dosync (conj! a {:id 1 :contents 1}))
    (println a)
    (persistent! a))
  => {1 {:id 1 :contents 1}})

(fact "an entry can only have one entry with the id"
  (let [a (DynaRec. [:contents])]
    (dosync
     (assoc! a 1 {:id 1 :contents 1})
     (persistent! (assoc! a 1 {:id 1 :contents 2}))))
  => {1 {:id 1 :contents 2}})


(let [a (DynaRec. [:contents])]
  (dosync
   (assoc! a 1 {:id 1 :contents 1})
   (assoc! a 2 {:id 2 :contents 2})
   (assoc! a 3 {:id 3 :contents 3}))

  (fact "counting"
    (count a) => 3)

  (fact "access and invoking"
    @(a 3) => {:id 3 :contents 3})

  #_(fact "seq"
    (seq a)
    => {1 {:id 1 :contents 1}
        2 {:id 2 :contents 2}
        3 {:id 3 :contents 3} })

  (fact "dissoc"
    (persistent!
     (dosync (dissoc! a 3))) =>
    {1 {:id 1 :contents 1} 2 {:id 2 :contents 2}} ))

(comment
  (use 'hara.data.iotam)
  (def a (DynaRec.))
  (dosync
   (assoc! a 1 {:id 1 :contents 1})
   (assoc! a 2 {:id 2 :contents 2})
   (assoc! a 3 {:id 3 :contents 3}))

  (add-watch a :a println)
  (iswap! (.valAt a 3) assoc  :a 1)
  (.getWatches a)

  (.removeWatch a :a)
  (iswap! (.valAt a 3) assoc  :a 1)
  (.getWatches a))

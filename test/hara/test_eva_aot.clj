(ns hara.test-eva-aot
  (:refer-clojure :exclude [swap! reset!])
  (:use midje.sweet
        [hara.fn :only [deref*]]
        [hara.data.evom :only [evom swap! reset! add-watches remove-watches]])
  (:import hara.data.Eva))

(fact "testing the DynaRec constructor"
  @(Eva.) => [])

(fact "simple specification"
  (let [ev (Eva.)]
    (conj! ev {:id :0})
    (facts "seq"
      (count ev) => 1
      @(first (.seq ev))   => {:id :0}
      @(first (seq ev))    => {:id :0})
    (facts "valAt"
      @(.valAt ev 0)       => {:id :0}
      @(.valAt ev 0 :na)   => {:id :0}
      (.valAt ev 1 :na)    => :na)
    (facts "nth"
      @(.nth ev 0)         => {:id :0}
      @(.nth ev 0 :na)     => {:id :0}
      (.nth ev 1)          => nil
      (.nth ev 1 :na)      => :na
      @(nth ev 0)          => {:id :0}
      @(nth ev 0 :na)      => {:id :0}
      (nth ev 1)           => nil
      (nth ev 1 :na)       => :na)
    (facts "persistent"
      (.persistent ev)     => [{:id :0}])
      (persistent! ev)     => [{:id :0}]))

(fact "testing append and pop operations"
  (against-background
    (before :checks (def a (Eva.))))

  (deref* (conj! a 1)) => [1]
  (deref*
   (-> a (conj! 0) (conj! 1))) => [0 1]
  (deref*
   (-> a (assoc! 0 1) (assoc! 1 1))) => [1 1]
  (deref*
   (-> a (conj! 0) (conj! 1) pop! pop!)) => []
  (deref*
   (-> a pop!)) => (throws Exception))

(facts "add-watch"
  (let [a      (Eva.)
        out    (evom nil)
        out-fn (fn [_ _ _ v & _] (reset! out (deref* v)))
        _      (add-watch a :a out-fn)]

    (fact "a is initialised"
      @a   => []
      @out => nil)

    (dosync (conj! a {:id 1 :contents 1}))
    (fact "conj! changes both a and out"
      (deref* a) => [{:id 1 :contents 1}]
      @(a 0) => {:id 1 :contents 1}
      @out => [{:id 1 :contents 1}])

    (dosync (conj! a {:id 2 :contents 2}))
    (fact "conj! changes both a and out"
      (deref* a) => [{:id 1 :contents 1}
                     {:id 2 :contents 2}]
      @(a 0) => {:id 1 :contents 1}
      @(a 1) => {:id 2 :contents 2}
      @out => [{:id 1 :contents 1}
               {:id 2 :contents 2}])

    (remove-watch a :a) ;; remove watch, <out> will not update when a is modified
    (dosync (conj! a {:id 3 :contents 3}))
    (fact "conj! changes both a and out"
      (deref* a) => [{:id 1 :contents 1}
                     {:id 2 :contents 2}
                     {:id 3 :contents 3}]
      @out => [{:id 1 :contents 1}
               {:id 2 :contents 2}])))

(facts "add-elem-watches"
  (let [a      (Eva.)
        out    (evom nil)
        out-fn (fn [_ _ _ v & _] (reset! out (deref* v)))
        _      (.addElemWatch a :a out-fn)]

    (dosync (conj! a {:id 1 :contents 1}))
    (fact "conj! does not affect out"
      (deref* a) => [{:id 1 :contents 1}]
      @(a 0) => {:id 1 :contents 1}
      @out => nil)

    (do (reset! (a 0) {:id 1 :contents 2}))
    (fact "reset! methods"
      (deref* a) => [{:id 1 :contents 2}]
      @(a 0) => {:id 1 :contents 2}
      @out => {:id 1 :contents 2})

    (swap! (.valAt a 0) assoc :a 1)
    (fact "swap! methods"
      (deref* a) => [{:id 1 :contents 2 :a 1}]
      @(a 0) => {:id 1 :contents 2 :a 1}
      @out => @(a 0)
      (:a (.getElemWatches a)) =not=> nil)

      ;; remove element watch
    (do (.removeElemWatch a :a)
        (swap! (.valAt a 0) assoc :a 2))
    (fact "remove watch"
      (deref* a) => [{:id 1 :contents 2 :a 2}]
      @(a 0) => {:id 1 :contents 2 :a 2}
      ;;@out =not=> @(a 0)
      (:a (.getElemWatches a)) => nil)))

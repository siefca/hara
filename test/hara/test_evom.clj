(ns hara.test-evom
  (:refer-clojure :exclude [swap! reset!])
  (:use midje.sweet
        hara.data.evom))

(facts "evom constructor"
  (let [inil (evom nil)
        i1  (evom 1)]
    (fact
      "inil has nil"
      @inil => nil)
    (fact
      "i1 has 1"
      @i1 => 1)))

(facts "reset! operation"
  (let [inil (evom nil)]
    (fact
      "inil has nil"
      @inil => nil)

    (do (reset! inil 1)) ;; set inil to 1
    (fact
      "inil now has 1"
      @inil => 1)

    (do (reset! inil nil)) ;; set inil back to nil
    (fact
      "inil has nil"
      @inil => nil)))

(facts "swap! operation"
  (let [inil (evom nil)]
    (fact
      "inil has nil"
      @inil => nil)

    (do (swap! inil (fn [_] 1))) ;; set inil to 1
    (fact
      "inil now has 1"
      @inil => 1)

    (do (swap! inil (fn [_]))) ;; set inil back to nil
    (fact
      "inil has nil"
      @inil => nil)))

(facts "watch operation"
  (let [itm      (evom nil)
        out1     (evom nil)
        out2     (evom nil)
        to-out1-fn (fn [k r p v t f args] (swap! out1 f))
        to-out2-fn (fn [k r p v t f args] (swap! out2 f))]

    (add-watch itm  :test to-out1-fn) ;; Add watch to itm to transform out1
    (add-watch out1 :test to-out2-fn) ;; Add watch to out1 to transform out2
    (fact "assert that itm and out are nil"
      @itm => nil
      @out1 => nil
      @out2 => nil)

    (swap! itm (fn [_] 1))
    (fact "assert that itm is 1 and out has been manipulated"
      @itm => 1
      @out1 => 1
      @out2 => 1)

    (swap! itm (fn [_]))
    (fact "assert that itm is 1 and out has been manipulated"
      @itm => nil
      @out1 => nil
      @out2 => nil)))

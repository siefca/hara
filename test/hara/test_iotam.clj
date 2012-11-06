(ns hara.test-iotam
  (:use midje.sweet
        hara.iotam))

(facts "iotam constructor"
  (let [inil (iotam nil)
        i1  (iotam 1)]
    (fact
      "inil has nil"
      @inil => nil)
    (fact
      "i1 has 1"
      @i1 => 1)))

(facts "ireset! operation"
  (let [inil (iotam nil)]
    (fact
      "inil has nil"
      @inil => nil)

    (do (ireset! inil 1)) ;; set inil to 1
    (fact
      "inil now has 1"
      @inil => 1)

    (do (ireset! inil nil)) ;; set inil back to nil
    (fact
      "inil has nil"
      @inil => nil)))

(facts "swap! operation"
  (let [inil (iotam nil)]
    (fact
      "inil has nil"
      @inil => nil)

    (do (iswap! inil (fn [_] 1))) ;; set inil to 1
    (fact
      "inil now has 1"
      @inil => 1)

    (do (iswap! inil (fn [_]))) ;; set inil back to nil
    (fact
      "inil has nil"
      @inil => nil)))

(facts "watch operation"
  (let [itm      (iotam nil)
        out1     (iotam nil)
        out2     (iotam nil)
        to-out1-fn (fn [k r p v t f args] (iswap! out1 f))
        to-out2-fn (fn [k r p v t f args] (iswap! out2 f))]

    (add-watch itm  :test to-out1-fn) ;; Add watch to itm to transform out1
    (add-watch out1 :test to-out2-fn) ;; Add watch to out1 to transform out2
    (fact "assert that itm and out are nil"
      @itm => nil
      @out1 => nil
      @out2 => nil)

    (iswap! itm (fn [_] 1))
    (fact "assert that itm is 1 and out has been manipulated"
      @itm => 1
      @out1 => 1
      @out2 => 1)

    (iswap! itm (fn [_]))
    (fact "assert that itm is 1 and out has been manipulated"
      @itm => nil
      @out1 => nil
      @out2 => nil)))

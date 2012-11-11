(ns hara.test-evom
  (:refer-clojure :exclude [swap! reset!])
  (:use midje.sweet
        hara.data.evom))

(defn is-evom [& [value]]
  (fn [evm]
    (if (and (instance? hara.data.Evom evm)
             (= @evm value))
      true)))

(facts "evom constructor"
  (let [inil (evom nil)
        i1  (evom 1)]
    (fact
      "inil has nil"
      inil => (is-evom nil))
    (fact
      "i1 has 1"
      i1 => (is-evom 1))))

(facts "reset! operation"
  (let [inil (evom nil)]
    (fact
      "inil has nil"
      inil => (is-evom nil))

    (do (reset! inil 1)) ;; set inil to 1
    (fact
      "inil now has 1"
      inil => (is-evom 1))

    (do (reset! inil nil)) ;; set inil back to nil
    (fact
      "inil has nil"
      inil => (is-evom nil))))

(facts "swap! operation"
  (let [inil (evom nil)]
    (fact
      "inil has nil"
      inil => (is-evom nil))

    (do (swap! inil (fn [_] 1))) ;; set inil to 1
    (fact
      "inil now has 1"
      inil => (is-evom 1))

    (do (swap! inil (fn [_]))) ;; set inil back to nil
    (fact
      "inil has nil"
      inil => (is-evom nil))))

(facts "watch operation"
  (let [itm      (evom nil)
        out1     (evom nil)
        out2     (evom nil)
        to-out1-fn (fn [k r p v t f args] (swap! out1 f))
        to-out2-fn (fn [k r p v t f args] (swap! out2 f))]

    (add-watch itm  :test to-out1-fn) ;; Add watch to itm to transform out1
    (add-watch out1 :test to-out2-fn) ;; Add watch to out1 to transform out2
    (fact "assert that itm and out are nil"
      itm => (is-evom nil)
      out1 => (is-evom nil)
      out2 => (is-evom nil))

    (swap! itm (fn [_] 1))
    (fact "assert that itm is 1 and out has been manipulated"
      itm => (is-evom 1)
      out1 => (is-evom 1)
      out2 => (is-evom 1))

    (swap! itm (fn [_]))
    (fact "assert that itm is 1 and out has been manipulated"
      itm => (is-evom nil)
      out1 => (is-evom nil)
      out2 => (is-evom nil))))

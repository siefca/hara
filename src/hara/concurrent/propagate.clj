(ns hara.concurrent.propagate
  (:require [clojure.string :as string]
            [hara.protocol.state :refer :all]
            [hara.common.error :refer [suppress]]
            [hara.common.checks :refer [atom? iref?]]
            [hara.common.state :as state]))

(def nothing ::nothing)

(defn nothing? [x]
  (= x nothing))

(defn straight-through [& [x]] x)

(defn cell-state
  [{:keys [label content ref-fn]}]
  (do {:label       (atom label)
       :content     ((or ref-fn atom) content)
       :propagators (atom #{})}))

(defn propagator-state
  [{:keys [label in-cells out-cell tf tdamp concurrent]}]
  (do {:label      (atom label)
       :in-cells   (atom in-cells)
       :out-cell   (atom out-cell)
       :tf         (atom tf)
       :tdamp      (atom (or tdamp =))
       :concurrent (atom concurrent)}))

(defprotocol IPropagate
  (propagate [pg]))

(defmacro with-concurrent [flag & body]
  `(if ~flag (do ~body) (future ~body)))

(defn propagation-transfer [in-vals {:keys [tf tdamp out-cell] :as pg}]
  (let [out (if-not (some nothing? in-vals)
              (suppress (apply tf in-vals) nothing)
              nothing)]
    (if-not (or (nothing? out)
                (suppress (tdamp @out-cell out)))
      (out-cell out))))

(defn format-cells [{:keys [label] :as cell}]
  (or label
      (format "{%s}" (.hashCode cell))))

(deftype Propagator [state]
  Object
  (toString [pg]
    (format "#pg%s :: %s => %s"
            (if-let [label (:label pg)] (name label) "")
            (str "[" (string/join " " (mapv format-cells (:in-cells pg))) "]")
            (format-cells (:out-cell pg))))

  IPropagate
  (propagate [pg]
    (let [in-vals (map deref (:in-cells pg))]
      (if (:concurrent pg)
        (future (propagation-transfer in-vals pg))
        (propagation-transfer in-vals pg))
      (:out-cell pg)))

  clojure.lang.ILookup
  (valAt [pg k] (if-let [res (get state k)]
                    (if (iref? res)
                      @res res)))
  (valAt [pg k not-found] (or (get pg k) not-found)))

(extend-type Propagator
  IStateSet
  (-set-state [pg k val]
    (if-let [res (get pg k)]
      (if (iref? res)
        (state/set res val)))
    pg))

(defmethod print-method Propagator
  [v w]
  (.write w (str v)))

(defprotocol CellProtocol
  (register-propagator [cell pg])
  (deregister-propagator [cell pg])
  (notify-propagators [cell]))

(deftype Cell [state]
  Object
  (toString [cell]
    (format "#cell%s[%s]"
            (if-let [label (:label cell)]
              (str "." label)
              "")
            (:content cell)))

  CellProtocol
  (register-propagator [cell pg]
    (swap! (:propagators state) conj pg)
    cell)
  (deregister-propagator [cell pg]
    (swap! (:propagators state) disj pg)
    cell)
  (notify-propagators [cell]
    (doseq [p (:propagators cell)]
      (propagate p)))

  IStateSet
  (-set-state [cell k val]
    (if-let [res (get state k)]
      (if (iref? res)
        (state/set res val)))
    cell)

  clojure.lang.ILookup
  (valAt [cell k] (if-let [res (get state k)]
                    (if (iref? res)
                      @res res)))
  (valAt [cell k not-found] (or (get cell k) not-found))

  clojure.lang.IDeref
  (deref [cell] (:content cell))

  clojure.lang.IFn
  (invoke [cell content]
    (state/set cell :content content)
    (notify-propagators cell)
    cell)

  clojure.lang.IRef
  (setValidator [cell vf] (.setValidator (:content state) vf))
  (getValidator [cell] (.getValidator (:content state)))
  (getWatches [cell] (.getWatches (:content state)))
  (addWatch [cell key callback] (add-watch (:content state) key callback))
  (removeWatch [cell key] (remove-watch (:content state) key)))

(defmethod print-method Cell
  [v w]
  (.write w (str v)))

(defn cell
  "creates a propogation cell

  (def cell-a (cell))
  @cell-a => :hara.concurrent.propagate/nothing

  (def cell-b (cell \"Hello\"))
  @cell-b => \"Hello\"

  (cell-b \"World\")    ;; invoking sets the state of the cell
  @cell-b => \"World\""
  {:added "2.1"}
  ([] (cell nothing))
  ([content] (cell content {}) )
  ([content opts]
     (Cell. (-> opts
                (assoc :content content)
                (cell-state)))))

(defn propagator
  ([label] (propagator label {:tf straight-through}))
  ([label {:keys [in-cells out-cell tf tdamp concurrent] :as opts}]
     (Propagator. (-> opts
                      (assoc :label label)
                      (propagator-state)))))

(defn link
  "creates a propogation link between a set of input cells and an output cell

  (def in-a  (cell 1))
  (def in-b  (cell 2))
  (def inter (cell))
  (def in-c  (cell 3))
  (def out   (cell))

  (link [in-a in-b] inter +)
  (link [inter in-c] out +)

  (in-a 10)
  @inter => 12
  @out => 15

  (in-b 100)
  @inter => 110
  @out => 113

  (in-c 1000)
  @inter => 110
  @out => 1110"
  {:added "2.1"}
  ([sources sink] (link sources sink straight-through))
  ([sources sink tf] (link sources sink tf {}))
  ([sources sink tf {:keys [label tdamp concurrent] :as options}]
     (let [pg (propagator label (assoc options
                                  :tf tf
                                  :in-cells sources
                                  :out-cell sink))]
       (doseq [s sources]
         (register-propagator s pg))
       pg)))

(defn unlink
  "removes the propagation link between a set of cells

  (def in-a  (cell 1))
  (def out   (cell))

  (def lk (link [in-a] out))
  (in-a 10)
  @out => 10

  (unlink lk)
  (in-a 100)
  @in-a 100
  @out => 10"
  {:added "2.1"}
  [pg]
  (let [sources (:in-cells pg)]
    (doseq [s sources]
      (deregister-propagator s pg))
    (state/set pg :in-cells [])
    (state/set pg :out-cell nil)))

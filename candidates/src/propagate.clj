(ns hara.concurrent.propagate
  (:require [hara.common.error :refer [suppress]]
            [hara.common.checks :refer [atom? aref? iref?]]
            [hara.protocol.stateful :refer [IStateful]]
            [hara.concurrent.state :refer [set-state]]))

(def nothing ::nothing)

(defn nothing? [x]
  (= x nothing))

(defn straight-through [& [x]] x)

(defn cell-state
  ([{:keys [label content ref-fn]}]
     {:label       (atom label)
      :content     (ref-fn content)
      :propagators (atom #{})}))

(defn propagator-state
  ([{:keys [label in-cells out-cell tf tdamp concurrent]}]
     {:label      (atom label)
      :in-cells   (atom in-cells)
      :out-cell   (atom out-cell)
      :tf         (atom tf)
      :tdamp      (atom (or tdamp =))
      :concurrent (atom concurrent)}))

(defprotocol PutProtocol
  (put! [mut k val]))

(defprotocol PropagatorProtocol
  (propagate! [pg]))

(deftype Propagator [state]
  PropagatorProtocol
  (propagate! [pg]
    (let [ins     (map deref (:in-cells pg))
          tf      (:tf pg)
          tdamp   (:tdamp pg)
          outcell (:out-cell pg)
          out     (if-not (some nothing? ins)
                    (suppress (apply tf ins) nothing)
                    nothing)]
      (if-not (or (nothing? out)
                  (suppress (tdamp @outcell out)))
          (outcell out))))

  clojure.lang.ILookup
  (valAt [pg k] (if-let [res (get state k)]
                    (if (iref? res)
                      @res res)))
  (valAt [pg k not-found] (or (get pg k) not-found)))


(extend-type
    PutProtocol
  (put! [pg k val]
    (if-let [res (get state k)]
      (if (iref? res)
        (set-state res val)))
    pg))


(defprotocol CellProtocol
  (register-propagator [cell pg])
  (deregister-propagator [cell pg])
  (notify-propagators [cell]))

(deftype Cell [state]
  CellProtocol
  (register-propagator [cell pg]
    (swap! (:propagators state) conj pg)
    cell)
  (deregister-propagator [cell pg]
    (swap! (:propagators state) disj pg)
    cell)
  (notify-propagators [cell]
    (doseq [p (:propagators cell)]
      (propagate! p)))

  PutProtocol
  (put! [cell k val]
    (if-let [res (get state k)]
      (if (iref? res)
        (set-state res val)))
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
    (put! cell :content content)
    (notify-propagators cell)
    cell)

  clojure.lang.IRef
  (setValidator [cell vf] (.setValidator (:content state) vf))
  (getValidator [cell] (.getValidator (:content state)))
  (getWatches [cell] (.getWatches (:content state)))
  (addWatch [cell key callback] (add-watch (:content state) key callback))
  (removeWatch [cell key] (remove-watch (:content state) key)))

(defn cell
  ([] (cell nothing))
  ([content] (cell nil content {}) )
  ([label content {:keys [label content ref-fn] :as options}]
     (Cell. (cell-state
             (assoc options
               :label label
               :content content)))))

(defn propagator
  ([label] (propagator label straight-through))
  ([label & {:keys [in-cells out-cell tf tdamp concurrent] :as options}]
     (Propagator. (propagator-state
                   (assoc options
                     :lobel label)))))

(defn connect
  ([sources sink] (connect nil sources sink straight-through))
  ([sources sink tf] (connect nil sources sink tf {}))
  ([sources sink tf & {:keys [label in-cells out-cell tdamp concurrent] :as options}]
     (let [pg (propagator (assoc options :tf tf))]
       (doseq [s sources]
         (register-propagator s pg))
       pg)))

(defn disconnect [pg]
  (let [sources (:in-cells pg)]
    (doseq [s sources]
      (deregister-propagator s pg))
    (put! pg :in-cells [])
    (put! pg :out-cell nil)))

(defn- label-or-hash [obj]
    [(or (:label obj) (.hashCode obj))])

(defmethod print-method
  Propagator
  [pg w]
  (print-method
   (let [hash (.hashCode pg)]
     (format "<P@%s %s => %s>"
             hash
             (->> (map label-or-hash (:in-cells pg))
                  (cons (or (:label pg) 'fn)) (apply list) str)
             (label-or-hash (:out-cell pg))))
   w))

(defmethod print-method
  Cell
  [cell w]
  (print-method
   (let [hash (.hashCode cell)]
     (format "<Cell@%s %s>"
             hash @cell))
   w))

(comment
  (defn network [inputs output f])

  (def a (cell 1))
  (def b (cell 1))
  (def c (cell 1))

  (def pg0 (connect [a b] c +) )

  (a 5)
  @c

  )

(ns hara.propagator.cell
  (:gen-class
   :name hara.propagator.Cell
   :prefix "-"
   :init init
   :constructors {[] []}
   :state state
   :extends clojure.lang.AFn
   :methods [   [reset [] void]
                [addPropagator [hara.propagator.Propagator] void]
                [removePropagator [hara.propagator.Propagator] void]
                [getPropagators [] clojure.lang.PersistentHashSet]
                [clearPropagators [] void]
                [notifyPropagators [] void]
                [getContent [] java.lang.Object]
                [setContent [java.lang.Object] void]
                [clearContent [] void]]
      :import [hara.propagator.Propagator]))

(def nothing ::nothing)

(defn content [this] (:content (.state this)))

(defn propagators [this] (:content (.state this)))

(defn -init
  ([]     [[]  {:content       (ref nothing)
                :propagators   (atom [])}]))

(defn -getContent
  [this]
  (deref (content this)))

(defn -setContent
  [this obj]
  (dosync (ref-set (content this) obj)))

(defn -setContent
  [this obj]
  (-setContent this nothing))

(defn -getPropagators
  [this]
  (deref (propagators this)))

(defn -clearPropagators
  [this]
  (reset! (propagators this) []))

(defn -addPropagator [this prg]
  (swap! (propagators this) conj prg))

(defn -removePropagator [this prg]
  (swap! (propagators this) disj prg))

(defn -reset [this]
  (-setContent this nothing)
  (-clearPropagators this))

(defn -notifyPropagators [this])

(defn -invoke
  ([this] (-getContent this))
  ([this val] (-setContent this val)))


(comment )

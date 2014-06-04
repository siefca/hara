(ns hara.protocol.state
  (:refer-clojure :exclude [get set]))

(defprotocol IGetState
  (-get-state [obj opts]))

(defprotocol ISetState
  (-update-state [obj opts f args])
  (-set-state [obj opts v]))

(defn get
  "Like deref but is extensible

  (p/get (atom 1)) => 1

  (p/get (ref 1)) => 1"
  {:added "2.1"}
  ([obj] (get obj nil))
  ([obj opts]
     (-get-state obj opts)))

(defn set
  "Like reset! but is extensible

  (let [a (atom nil)]
    (p/set a 1)
    @a) => 1"
  {:added "2.1"}
  ([obj v] (set obj nil v))
  ([obj opts v]
     (-set-state obj opts v)
     obj))

(defn update
  "Like swap! but is extensible

  (let [a (atom 0)]
    (p/update a + 1)
    @a) => 1

  "
  {:added "2.1"}
  ([obj f]
     (update obj nil f [])
     obj)
  ([obj opts? f & args]
     (if (nil? opts?)
       (-update-state obj nil f (first args))
       (let [[opts f args]
             (if (fn? opts?)
               [nil opts? (cons f args)]
               [opts? f args])]
         (-update-state obj opts f args)
         obj))))

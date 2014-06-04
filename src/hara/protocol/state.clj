(ns hara.protocol.state
  (:refer-clojure :exclude [get set]))

(defprotocol IStateful
  (-get [obj opts])
  (-update [obj opts f args])
  (-set [obj opts v]))

(defn get
  "Like deref but is extensible through the IStateful protocol

  (p/get (atom 1)) => 1
  
  (p/get (ref 1)) => 1"
  {:added "2.1"}
  ([obj] (get obj nil))
  ([obj opts]
     (-get obj opts)))

(defn set
  "Like reset! but is extensible through the IStateful protocol

  (let [a (atom nil)]
    (p/set a 1)
    @a) => 1"
  {:added "2.1"}
  ([obj v] (set obj nil v))
  ([obj opts v]
     (-set obj opts v)
     obj))

(defn update
  "Like swap! but is extensible through the IStateful protocol

  (let [a (atom 0)]
    (p/update a inc)
    @a) => 1"
  {:added "2.1"}
  ([obj f]
     (update obj nil f [])
     obj)
  ([obj opts? f & args]
     (if (nil? opts?)
       (apply -update obj nil f args)
       (let [[opts f args]
             (if (fn? opts?)
               [nil opts? (cons f args)]
               [opts? f args])]
         (apply -update obj opts f args)
         obj))))

(ns hara.protocol.state
  (:refer-clojure :exclude [get set]))

(defprotocol IStateful
  (-get [obj opts])
  (-update [obj opts f args])
  (-set [obj opts v]))

(defn get
  "get"
  ([obj] (get obj nil))
  ([obj opts]
     (-get obj opts)))

(defn set
  "set"
  ([obj v] (set obj nil v))
  ([obj opts v]
     (-set obj opts v)
     obj))

(defn update
  "update"
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

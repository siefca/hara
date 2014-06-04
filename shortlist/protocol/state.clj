(ns hara.protocol.state
  (:refer-clojure :exclude [get]))

(defprotocol IStateful
  (-get [obj opts])
  (-update [obj opts f args])
  (-set [obj opts v]))

(defn get-state
  ([obj] (-get-state obj nil))
  ([obj opts]
     (-get-state obj opts)))

(defn set-state
  ([obj v] (-set-state obj nil v))
  ([obj opts v]
     (-set-state obj opts v)
     obj))

(defn update-state
  ([obj f]
     (update-state obj nil f [])
     obj)
  ([obj opts? f & args]
     (let [[opts f args]
           (if (fn? opts?)
             [nil opts? (cons f args)]
             [opts? f args])]
       (-update-state obj opts f args)
       obj)))
(ns hara.common.state
  (:require [hara.common.error :refer [error]]
            [hara.common.checks :refer [promise?]]
            [hara.protocol.state :refer :all])
  (:refer-clojure :exclude [get set]))

(defn get
  "Like deref but is extensible through the IStateGet protocol

  (state/get (atom 1)) => 1

  (state/get (ref 1)) => 1"
  {:added "2.1"}
  ([obj] (get obj nil))
  ([obj opts]
     (-get-state obj opts)))

(defn set
  "Like reset! but is extensible through the IStateSet protocol

  (let [a (atom nil)]
    (state/set a 1)
    @a) => 1"
  {:added "2.1"}
  ([obj v] (set obj nil v))
  ([obj opts v]
     (-set-state obj opts v)
     obj))

(defn update
  "Like swap! but is extensible through the IStateSet protocol

  (let [a (atom 0)]
    (state/update a + 1)
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

(defn dispatch
  "Updates the value contained within a container using another thread.

  (let [res (state/dispatch (atom 0)
                (fn [x]  (inc x)))]
    res   => future?
    @res  => atom?
    @@res => 1)"
  {:added "2.1"}
  [ref f & args]
  (future
    (apply update ref f args)))

(extend-type clojure.lang.IDeref
  IStateGet
  (-get-state [obj _]
    (deref obj)))

(extend-type clojure.lang.Atom
  IStateSet
  (-set-state [obj _ v]
    (reset! obj v))

  (-update-state [obj _ f args ]
    (apply swap! obj f args)))

(extend-type clojure.lang.Ref
  IStateSet
  (-set-state [obj _ v]
    (dosync (ref-set obj v)))

  (-update-state [obj _ f args]
    (dosync (apply alter obj f args))))

(extend-type clojure.lang.Agent
  IStateSet
  (-set-state [obj _ v]
    (send obj (fn [_] v)))

  (-update-state [obj _ f args]
    (println obj f args)
    (apply send obj f args)))

(extend-type clojure.lang.IPending
  IStateGet
  (-get-state [obj _]
    (if (.isRealized obj)
      (deref obj)))

  IStateSet
  (-set-state [obj _ v]
    (cond (.isRealized obj)
          (error "Already realised: " obj)

          (promise? obj)
          (deliver obj v)

          :else
          (error "Cannot set state for: " obj)))

  (-update-state [obj _ f args]
    (cond (.isRealized obj)
          (error "Already realised: " obj)

          (promise? obj)
          (deliver obj (apply f nil args))

          :else
          (error "Cannot set state for: " obj))))

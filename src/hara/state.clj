(ns hara.state
  (:require [hara.common.error :refer [error]]
            [hara.common.checks :refer [promise?]]
            [hara.protocol.state :refer [IStateful] :as p ]))

(extend-type clojure.lang.Atom
  IStateful
  (-get [obj _]
    (deref obj))

  (-set [obj _ v]
    (reset! obj v))

  (-update [obj _ f args ]
    (apply swap! obj f args)))

(extend-type clojure.lang.Ref
  IStateful
  (-get [obj _]
    (deref obj))

  (-set-state [obj _ v]
    (dosync (ref-set obj v)))

  (-update-state [obj _ f args]
    (dosync (apply alter obj f args))))

(extend-type clojure.lang.Agent
  IStateful
  (-get [obj _]
    (deref obj))

  (-set [obj _ v]
    (send obj (fn [_] v)))

  (-update [obj _ f args]
    (println obj f args)
    (apply send obj f args)))

(extend-type clojure.lang.IPending
  IStateful
  (-get [obj _]
    (if (.isRealized obj)
      (deref obj)))

  (-set [obj _ v]
    (cond (.isRealized obj)
          (error "Already realised: " obj)

          (promise? obj)
          (deliver obj v)

          :else
          (error "Cannot set state for: " obj)))

  (-update [obj _ f args]
    (cond (.isRealized obj)
          (error "Already realised: " obj)

          (promise? obj)
          (deliver obj (apply f nil args))

          :else
          (error "Cannot set state for: " obj))))

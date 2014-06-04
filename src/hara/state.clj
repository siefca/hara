(ns hara.state
  (:require [hara.common.error :refer [error]]
            [hara.common.checks :refer [promise?]]
            [hara.protocol.state :refer [ISetState IGetState]]))

(extend-type clojure.lang.IDeref
  IGetState
  (-get-state [obj _]
    (deref obj)))

(extend-type clojure.lang.Atom
  ISetState
  (-set-state [obj _ v]
    (reset! obj v))

  (-update-state [obj _ f args ]
    (apply swap! obj f args)))

(extend-type clojure.lang.Ref
  ISetState
  (-set-state [obj _ v]
    (dosync (ref-set obj v)))

  (-update-state [obj _ f args]
    (dosync (apply alter obj f args))))

(extend-type clojure.lang.Agent
  ISetState
  (-set-state [obj _ v]
    (send obj (fn [_] v)))

  (-update-state [obj _ f args]
    (println obj f args)
    (apply send obj f args)))

(extend-type clojure.lang.IPending
  IGetState
  (-get-state [obj _]
    (if (.isRealized obj)
      (deref obj)))

  ISetState
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

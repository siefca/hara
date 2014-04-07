(ns hara.state
  (:require [hara.common.error :refer [error]]
            [hara.common.checks :refer [atom? aref? agent? promise?]]
            [hara.protocol.stateful :refer [IStateful] :as p]
            [clojure.string :as string]))

(extend-type clojure.lang.Atom
  IStateful
  (p/-get-state [obj _]
    (deref obj))

  (p/-set-state [obj _ v]
    (reset! obj v))

  (p/-update-state [obj _ f args ]
    (apply swap! obj f args)))

(extend-type clojure.lang.Ref
  IStateful
  (p/-get-state [obj _]
    (deref obj))

  (p/-set-state [obj _ v]
    (dosync (ref-set obj v)))

  (p/-update-state [obj _ f args]
    (dosync (apply alter obj f args))))

(extend-type clojure.lang.Agent
  IStateful
  (p/-get-state [obj _]
    (deref obj))

  (p/-set-state [obj _ v]
    (send obj (fn [_] v)))

  (p/-update-state [obj _ f args]
    (apply send obj f args)))

(extend-type clojure.lang.IPending
  IStateful
  (p/-get-state [obj _]
    (if (.isRealized obj)
      (deref obj)))

  (p/-set-state [obj _ v]
    (cond (.isRealized obj)
          (error "Already realised: " obj)

          (promise? obj)
          (deliver obj v)

          :else
          (error "Cannot set state for: " obj)))

  (p/-update-state [obj _ f args]
    (cond (.isRealized obj)
          (error "Already realised: " obj)

          (promise? obj)
          (deliver obj (apply f args))

          :else
          (error "Cannot set state for: " obj))))

(defn dispatch
  "Updates the value contained within a stateful container using another thread.

    (dispatch! (atom 0)
                (fn [x] (Thread/sleep 1000)
                        (inc x)))
    ;=> <future_call>
  "
  ([rf f]
     (future (p/update-state rf f)))
  ([rf opt? f & args]
     (future (apply p/update-state rf opt? f args))))

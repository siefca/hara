(ns hara.concurrent.notification
  (:require [hara.common.hash :refer [hash-label]]
            [hara.common.watch :as watch]
            [hara.common.state :as state]))

(defn dispatch
  "Updates the value contained within a ref or atom using another thread.
  @@(dispatch (atom 0)
              (fn [x]
                (Thread/sleep 200)
                (inc x)))
  => 1"
  {:added "2.1"}
  ([obj f]
    (future (state/update obj f)))
 ([obj opts? f & args]
    (future (apply state/update obj opts? f args))))

(defn notify
  "Creates a watch mechanism so that when a long running function
  finishes, it returns a promise that delivers the updated iref.
  (let [res (notify #(do (Thread/sleep 200)
                         (state/update % inc))
                    (ref 1))]
    res   => promise?
    @res  => iref?
    @@res => 2)"
  {:added "2.1"}
  ([mtf rf] (notify mtf rf nil))
  ([mtf rf opts]
     (let [p  (promise)
           pk (keyword (hash-label p))]
       (watch/add rf pk
                  (fn [_ _ _ _]
                    (watch/remove rf pk opts)
                    (deliver p rf))
                  opts)
       (mtf rf)
       p)))

(defn wait-on
  "Waits for a long running multithreaded function to update the
  ref. Used for testing purposes

  (let [atm (atom 0)
        f (fn [obj] (dispatch obj #(do (Thread/sleep 300)
                                      (inc %))))]
    (wait-on f atm)
    @atm => 1)"
  {:added "2.1"}
  ([mtf rf]
     (deref (notify mtf rf)))
  ([mtf rf opts ms ret]
     (deref (notify mtf rf opts) ms ret)))

(defn alter-on
  "A redundant function. Used for testing purposes. The same as
   `(alter! ref f & args)` but the function is wired with the
   notification scheme.
  (let [atm (atom 0)
        _   (alter-on atm #(do (Thread/sleep 300)
                               (inc %)))]
    @atm => 1)"
  {:added "2.1"}
  ([obj f]
     (wait-on #(dispatch % f) obj))
  ([obj opts? f & args]
     (wait-on #(apply dispatch % opts? f args) obj)))

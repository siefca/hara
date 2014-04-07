<<<<<<< Local Changes

(defn change-watch-fn
  [sel f]
  (fn [k rf p n]
    (let [pv (get-> p sel)
          nv (get-> n sel)]
      (if-not (or (= pv nv) (nil? nv))
        (f k rf pv nv)))))

(defn add-change-watch
  "Adds a watch function that only triggers when there is change
   in `(sel <value>)`.

    (def subject (atom {:a 1 :b 2}))
    (def observer (atom nil)
    (add-change-watch subject :clone
        :b (fn [& _] (reset! observer @a)))

    (swap! subject assoc :a 0)
    @observer => nil

    (swap! subject assoc :b 1)
    @observer => {:a 0 :b 1}
  "
  ([rf k f] (add-change-watch rf k identity f))
  ([rf k sel f]
     (add-watch rf k (change-watch-fn sel f))))

 (defn notify-fn
   "Returns a watch-callback function that waits
    for the ref to be updated then removes itself
    and delivers the promise"
   [p pk]
   (fn [_ ref _ _]
     (remove-watch ref pk)
     (deliver p ref)))

 (defn notify-promise
   "Returns a notifier to a long running function so that it returns
    a promise that is accessible when the function has finished.
    updating the iref.

     (let [res (notifier
                  #(future (sleep 2000)
                           (update-val % inc))
                 (atom 1)
                 notify-on-all)]
     res ;=> promise?
     @res ;=> atom?
     @@res ;=> 2)
   "
   [mtf ref notify-fn]
   (let [p  (promise)
         pk (hash-keyword p)]
     (add-watch ref pk (notify-fn p pk))
     (mtf ref)
     p))

 (defn wait
   "Waits for a long running multithreaded function to update the ref.
    Used for testing purposes

     (def atm (atom 1))
     ;; concurrent call
     (def f #(dispatch! % slow-inc))
     (def ret (wait f atm))

     @atm ;=> 2
     @ret ;=> 2
   "
   ([mtf ref]
      (deref (notify-promise mtf ref notify-fn)))
   ([mtf ref ms ret]
      (deref (notify-promise mtf ref notify-fn) ms ret)))
=======
(ns src.observable)
>>>>>>> External Changes

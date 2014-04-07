(ns hara.concurrent.latch
  (:require [hara.concurrent.state :refer [hash-keyword add-change-watch]]
            [clojure.string :as string]))

(defn latch-fn
  [rf f]
  (fn [_ _ _ v]
    (set-state rf (f v))))

(defn latch
  "Followes two irefs together so that when `master`
   changes, the `slave` will also be updated

    (def master (atom 1))
    (def slave (atom nil))

    (follow master slave #(* 10 %)
    (swap! master inc)
    @master ;=> 2
    @slave ;=> 20
  "
  ([master slave] (latch master slave identity))
  ([master slave f]
     (add-watch master
                (hash-keyword master slave)
                (latch-fn slave f))))

(defn latch-changes
  "Same as latch but only changes in `(sel <val>)` will be propagated
    (def master (atom {:a 1))
    (def slave (atom nil))

    (latch-changes master slave :a #(* 10 %)
    (swap! master update-in [:a] inc)
    @master ;=> {:a 2}
    @slave ;=> 20
  "
  ([master slave] (latch-changes master slave identity identity))
  ([master slave sel] (latch-changes master slave sel identity))
  ([master slave sel f]
     (add-change-watch master (hash-keyword master slave)
                       sel (latch-fn slave f))))

(defn unlatch
  "Removes the latch so that updates will not be propagated"
  [master slave]
  (remove-watch master (hash-keyword master slave)))

(comment
  (def a (agent 1))
  (def b (agent 1))

  (set-state a 3)

  (>pst)

  (add-watch a :k (fn [_ _ _ _]
                    (println "hello")))
  (follow a b)

  (.getWatches a)

  (update-val a inc)
  @b
)

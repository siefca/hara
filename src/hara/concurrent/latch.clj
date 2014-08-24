(ns hara.concurrent.latch
  (:require [hara.common.hash :refer [hash-label]]
            [hara.common.watch :as watch]
            [hara.common.state :as state]
            [clojure.string :as string]))

(defn latch-fn
  [rf f]
  (fn [_ _ _ v]
    (state/set rf (f v))))

(defn latch
  "Followes two irefs together so that when `master`
  changes, the `slave` will also be updated.

  (def master (atom 1))
  (def slave (atom nil))

  (latch master slave #(* 10 %))
  (swap! master inc)

  @master => 2
  @slave => 20"
  {:added "2.1"}
  ([master slave] (latch master slave identity))
  ([master slave f] (latch master slave f nil))
  ([master slave f opts]
     (watch/add master
                (keyword (hash-label master slave))
                (latch-fn slave f)
                opts)))

(defn unlatch
  "Removes the latch so that updates will not be propagated

  (def master (atom 1))
  (def slave (atom nil))

  (latch master slave)
  (swap! master inc)
  @master => 2
  @slave => 2
  
  (unlatch master slave)
  (swap! master inc)
  @master => 3
  @slave => 2"
  {:added "2.1"}
  [master slave]
  (watch/remove master (keyword (hash-label master slave))))

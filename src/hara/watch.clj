(ns hara.watch
  (:use [hara.fn :only [look-up]]))

(defn change-watch-fn [kv f]
  (fn [k rf p n]
    (let [pv (look-up p kv)
          nv (look-up n kv)]
      (cond (and (nil? pv) (nil? nv)) nil
            (= pv nv) nil
            :else (f k rf pv nv)))))


(comment
  (look-up {:a {:b {:c :d}}} [:a :b])
  (def wc (change-watch-fn [:a] println))
  (def b (atom {:a 1 :b 0}))
  (add-watch b :w wc)
  (swap! b assoc :b 1)
  (swap! b assoc :a 2)
  (swap! b assoc :a 3))

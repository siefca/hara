(ns hara.watch)

(defn- call-if-not-nil [f v]
  (if-not (nil? v) (f v)))

(defn- lookup [m ks]
  (reduce (fn [acc f] (call-if-not-nil #(f %) acc))
          m
          ks))

(defn change-watch-fn [kv f]
  (fn [k rf p n]
    (let [pv (lookup p kv)
          nv (lookup n kv)]
      (cond (and (nil? pv) (nil? nv)) nil
            (= pv nv) nil
            :else (f k rf pv nv)))))


(comment
  (lookup {:a {:b {:c :d}}} [:a :b])
  (def wc (change-watch-fn [:a] println))
  (def b (atom {:a 1 :b 0}))
  (add-watch b :w wc)
  (swap! b assoc :b 1)
  (swap! b assoc :a 2)
  (swap! b assoc :a 3))

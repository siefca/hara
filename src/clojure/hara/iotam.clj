(ns hara.iotam
  (:import hara.data.Iotam)
  (:use [hara.fn :only [look-up]]))

(defn iotam [obj]
  (Iotam. obj))

(defn iswap! 
  ([^hara.data.Iotam iotam f] (.swap iotam f))
  ([^hara.data.Iotam iotam f x] (.swap iotam f x))
  ([^hara.data.Iotam iotam f x y] (.swap iotam f x y))
  ([^hara.data.Iotam iotam f x y & args] (.swap iotam f x y args)))

(defn ireset!
  [^hara.data.Iotam iotam v] (.reset iotam v))

(defn iwatch-for-change [kv f]
  (fn [k rf p n t func args]
    (let [pv (look-up p kv)
          nv (look-up n kv)]
      (cond (and (nil? pv) (nil? nv)) nil
            (= pv nv) nil
            :else (f k rf pv nv t func args)))))


(comment
  (look-up {:a {:b {:c :d}}} [:a :b])
  (def wc (change-watch-fn [:a] println))
  (def b (atom {:a 1 :b 0}))
  (add-watch b :w wc)
  (swap! b assoc :b 1)
  (swap! b assoc :a 2)
  (swap! b assoc :a 3))

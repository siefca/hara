(ns hara.iotam
  (:import hara.data.Iotam))

(defn iotam [obj]
  (Iotam. obj))

(defn iswap! 
  ([^hara.data.Iotam iotam f] (.swap iotam f))
  ([^hara.data.Iotam iotam f x] (.swap iotam f x))
  ([^hara.data.Iotam iotam f x y] (.swap iotam f x y))
  ([^hara.data.Iotam iotam f x y & args] (.swap iotam f x y args)))

(defn ireset!
  [^hara.data.Iotam iotam v] (.reset iotam v))


(comment
  (look-up {:a {:b {:c :d}}} [:a :b])
  (def wc (change-watch-fn [:a] println))
  (def b (atom {:a 1 :b 0}))
  (add-watch b :w wc)
  (swap! b assoc :b 1)
  (swap! b assoc :a 2)
  (swap! b assoc :a 3))

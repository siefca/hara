(ns hara.data.iotam
  (:import hara.data.Iotam))

(defn iotam [obj]
  (Iotam. obj))

(defn iswap! 
  ([iotam f] (.swap iotam f))
  ([iotam f x] (.swap iotam f x))
  ([iotam f x y] (.swap iotam f x y))
  ([iotam f x y & args] (.swap iotam f x y args)))

(defn ireset!
  [iotam v] (.reset iotam v))
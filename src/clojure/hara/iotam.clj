(ns hara.iotam
  (:import hara.data.Iotam))

(defn iotam []
  (Iotam. obj))

(defn iswap!
  ([^hara.data.Iotam iotam f] (.swap iotam f))
  ([^hara.data.Iotam iotam f x] (.swap iotam f x))
  ([^hara.data.Iotam iotam f x y] (.swap iotam f x y))
  ([^hara.data.Iotam iotam f x y & args] (.swap iotam f x y args)))

(defn ireset!
  [^hara.data.Iotam iotam v] (.reset iotam v))

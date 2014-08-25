(ns hara.coerce.core)

(defmulti -coerce (fn [x y] [(type x) (type y)]))

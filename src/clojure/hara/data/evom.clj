(ns hara.data.evom
  (:refer-clojure :exclude [swap! reset!])
  (:import hara.data.Evom))

(defn add-watches [om watches]
  (doseq [w watches]
    (add-watch om (first w) (second w))))

(defn remove-watches [om ks]
  (doseq [k ks]
    (remove-watch om k)))

(defn swap!
  ([om f] (.swap om f))
  ([om f x] (.swap om f x))
  ([om f x y] (.swap om f x y))
  ([om f x y & args] (.swap om f x y args)))

(defn reset!
  [om v] (.reset om v))


(defn evom
  ([] (Evom. nil))
  ([obj] (Evom. obj)))

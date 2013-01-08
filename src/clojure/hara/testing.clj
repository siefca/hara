(ns hara.testing)

(defn is-atom [& [value]]
  (fn [at]
    (if (and (instance? clojure.lang.Atom at)
             (= @at value))
      true)))

(defn is-ref [& [value]]
  (fn [rf]
    (if (and (instance? clojure.lang.Ref rf)
             (= @rf value))
      true)))

(defn is-ova [& values]
  (fn [ov]
    (if (and (instance? hara.data.Ova ov)
             (= (seq (persistent! ov)) values))
      true)))

(defn has-length [lens]
  (fn [x]
    (and (sequential? x)
         (some #(= % (count x)) lens))))

(defn has-items [ks items]
  (fn [coll]
    (= items (map #(select-keys % ks) (vals coll)))))

(defn has-keys [ks]
  (fn [m]
    (let [s (apply hash-set (keys m))]
      (every? s ks))))

;; urls for ring

(defn url-request [url & [method params]]
  {:request-method (or method :get) :uri url :params params})


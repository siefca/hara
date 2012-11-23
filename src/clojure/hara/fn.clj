(ns hara.fn)

(defn call-if-not-nil [f v]
  (if-not (nil? v) (f v)))

(defn look-up [m ks]
  (reduce (fn [acc f] (call-if-not-nil #(f %) acc))
          m
          ks))

(defn watch-for-change [kv f]
  (fn [k rf p n & xs] ;; xs := [t func args]
    (let [pv (look-up p kv)
          nv (look-up n kv)]
      (cond (and (nil? pv) (nil? nv)) nil
            (= pv nv) nil
            :else (apply f k rf pv nv xs)))))

(defn manipulate*
  ([f x] (manipulate* f x {}))
  ([f x cs]
     (let [m-fn    #(manipulate* f % cs)
           pred-fn (fn [pd]
                     (cond (instance? Class pd) #(instance? pd %)
                           (fn? pd) pd
                           :else (constantly false)))
           custom? #((pred-fn (:pred %)) x)
           c (first (filter custom? cs))]
       (cond (not (nil? c))
             (let [ctor (or (:ctor c) identity)
                   dtor (or (:dtor c) identity)]
               (ctor (manipulate* f (dtor x) cs)))

             :else
             (cond
               (instance? clojure.lang.Atom x)   (atom (m-fn @x))
               (instance? clojure.lang.Ref x)    (ref (m-fn @x))
               (instance? clojure.lang.Agent x)  (agent (m-fn @x))
               (list? x)                         (apply list (map m-fn x))
               (vector? x)                       (vec (map m-fn x))

               (instance? clojure.lang.ISeq x)
               (map m-fn x)

               (instance? clojure.lang.IPersistentSet x)
               (apply hash-set (map m-fn x))

               (instance? clojure.lang.IPersistentMap x)
               (reduce #(apply assoc %1 %2) {} (map m-fn x))

               :else (f x))))))

(defn deref+ [x]
  (cond
   (instance? clojure.lang.IDeref x) @x
   :else x))

(defn deref*
  ([x] (deref* identity x))
  ([f x] (deref* f x []))
  ([f x cs]
     (manipulate* f
                  x
                  (conj cs {:pred clojure.lang.IDeref
                            :ctor identity
                            :dtor deref}))))

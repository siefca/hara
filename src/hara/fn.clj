(ns hara.fn
  (:refer-clojure :exclude [send]))

;; watch

(defn watch-for-change [ks f]
  (fn [k rf p n & xs] ;; xs := [t func args]
    (let [pv (get-in p ks)
          nv (get-in n ks)]
      (cond (and (nil? pv) (nil? nv)) nil
            (= pv nv) nil
            :else (apply f k rf pv nv xs)))))

(defn watch-elem-for-change [ks f]
  (fn [k ov rf p n]
    (let [pv (get-in p ks)
          nv (get-in n ks)]
      (if (not= pv nv) 
        (f k ov rf pv nv)))))


;; multi-threaded
(defmacro exec-seq [threaded bindings & body]
  `(cond (= ~threaded :single)
         (doseq ~bindings ~@body)
         (= ~threaded :multi)
         (doseq ~bindings (future ~@body))))

(ns hara.checkers
  (:use [hara.common :only [eq-chk]]
   ))

(defn is-type-fn [t chk]
  (fn [obj]
    (if (and (instance? t obj)
             (if chk
               (eq-chk @obj (or chk (sequence chk)))))
      true)))

(defn is-iref [& [chk]]
  (is-type-fn clojure.lang.IRef chk))

(defn is-atom [& [chk]]
  (is-type-fn clojure.lang.Atom chk))

(defn is-ref [& [chk]]
  (is-type-fn clojure.lang.Ref chk))

(defn is-ova [& [chk]]
  (fn [ov]
    (if (and (instance? hara.ova.Ova ov)
             (let [schk (or chk (sequence chk))]
               (eq-chk (persistent! ov) schk)))
      true)))

(defn has-keys [ks]
  (fn [m]
    (let [s (apply hash-set (keys m))]
      (every? s ks))))

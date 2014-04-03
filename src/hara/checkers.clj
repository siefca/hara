(ns hara.checkers
  (:require [hara.fn :refer [check]]))

(defn is-type-fn [t chk]
  (fn [obj]
    (if (and (instance? t obj)
             (or (= chk @obj)
                 (if chk
                   (check @obj (or chk (sequence chk))))))
      true)))

(defn is-iref [& [chk]]
  (is-type-fn clojure.lang.IRef chk))

(defn is-atom [& [chk]]
  (is-type-fn clojure.lang.Atom chk))

(defn is-ref [& [chk]]
  (is-type-fn clojure.lang.Ref chk))

(defn has-keys [ks]
  (fn [m]
    (let [s (apply hash-set (keys m))]
      (every? s ks))))

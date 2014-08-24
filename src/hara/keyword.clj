(ns hara.string.path
  (:require [clojure.string :as string]
            [hara.common.string :as common])
  (:refer-clojure :exclude [split join contains val]))

(defn join
  "Merges a sequence of keywords into one.

    (keyword/join [:hello :there])
    ;=> :hello/there

    (keyword/join [:a :b :c :d])
    ;=> :a/b/c/d)"
  ([ks] (join ks "/"))
  ([ks sep]
     (if (empty? ks) nil
         (let [meta (common/to-meta (first ks))]
           (->> (filter identity ks)
                (map common/to-string)
                (string/join sep)
                (common/from-string meta))))))

(defn split
  "The opposite of `join`. Splits a keyword
   by the `/` character into a vector of keys.

    (keyword-split :hello/there)
    ;=> [:hello :there]

    (keyword-split :a/b/c/d)
    ;=> [:a :b :c :d]
  "
  ([k] (split k #"/"))
  ([k re]
     (cond (nil? k) []

           :else
           (let [meta (common/to-meta k)]
             (mapv #(common/from-string meta %)
                   (string/split (common/to-string k) re))))))

(defn contains
  "Returns `true` if the first part of `k` contains `subk`

    (keyword-contains? :a :a)
    ;=> true

    (keyword-contains? :a/b/c :a/b)
    ;=> true
  "
  [k subk]
  (or (= k subk)
      (.startsWith (common/to-string k)
                   (str (common/to-string subk) "/"))))

(defn path-vec
  "Returns the namespace vector of keyword `k`.

    (keyword-nsvec :hello/there)
    ;=> [:hello]

    (keyword-nsvec :hello/there/again)
    ;=> [:hello :there]
  "
  [k]
  (or (butlast (split k)) []))

(defn path-vec?
  "Returns `true` if keyword `k` has the namespace vector `nsv`."
  [k pv]
  (= pv (path-vec k)))

(defn path-ns
  "Returns the namespace of `k`.

    (keyword-ns :hello/there)
    ;=> :hello

    (keyword-ns :hello/there/again)
    ;=> :hello/there
  "
  [k]
  (join (path-ns k)))

(defn path-ns?
  "Returns `true` if keyword `k` has a namespace or
   if `ns` is given, returns `true` if the namespace
   of `k` is equal to `ns`.

    (keyword-ns? :hello)
    ;=> false

    (keyword-ns? :hello/there)
    ;=> true

    (keyword-ns? :hello/there :hello)
    ;=> true
  "
  ([k] (< 0 (.indexOf (str k) "/")))
  ([k ns] (if-let [tkns (path-ns k)]
            (= 0 (.indexOf (str k)
                 (str ns "/")))
            (nil? ns))))

(defn path-root
  "Returns the namespace root of `k`.

    (keyword-root :hello/there)
    ;=> :hello

    (keyword-root :hello/there/again)
    ;=> :hello
  "
  [k]
  (first (path-ns k)))

(defn path-root?
  "Returns `true` if keyword `k` has the namespace base `nsk`."
  [k pk]
  (= pk (path-root k)))

(defn path-stem-vec
  "Returns the stem vector of `k`.

    (keyword-stemvec :hello/there)
    ;=> [:there]

    (keyword-stemvec :hello/there/again)
    ;=> [:there :again]
  "
  [k]
  (rest (split k)))

(defn path-stem-vec?
  "Returns `true` if keyword `k` has the stem vector `kv`."
  [k kv]
  (= kv (path-stem-vec k)))

(defn path-stem
  "Returns the steam of `k`.

    (keyword-stem :hello/there)
    ;=> :there

    (keyword-stem :hello/there/again)
    ;=> :there/again
  "
  [k]
  (join (path-stem-vec k)))

(defn path-stem?
  "Returns `true` if keyword `k` has the stem `kst`."
  [k ks]
  (= ks (path-stem k)))

(defn val
  "Returns the keyword value of the `k`.

    (keyword-val :hello)
    ;=> :hello

    (keyword-val :hello/there)
    ;=> :there"
   [k]
  (last (split k)))

(defn val?
  "Returns `true` if the keyword value of `k` is equal
   to `z`."
  [k z]
  (= z (val k)))

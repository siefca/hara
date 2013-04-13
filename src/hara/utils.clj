(ns hara.utils
  (:require [clojure.string :as st]
            [clojure.set :as set]))

;; ## String Methods

(defn replace-all
  "Returns a string with all instances of `old` in `s` replaced with
   the value of `new`."
  [s old new]
  (.replaceAll s old new))

(defn starts-with
  "Returns `true` if `s` begins with `pre`."
  [s pre]
  (.startsWith s pre))

(defn queue []
  (clojure.lang.PersistentQueue/EMPTY))

;; ## Type Predicates

(defn boolean?
  "Returns `true` if `x` is of type `java.lang.Boolean`."
  [x] (instance? java.lang.Boolean x))

(defn hash-map?
  "Returns `true` if `x` implements `clojure.lang.IPersistentMap`."
  [x] (instance? clojure.lang.IPersistentMap x))

(defn hash-set?
  "Returns `true` if `x` implements `clojure.lang.IPersistentHashSet`."
  [x] (instance? clojure.lang.PersistentHashSet x))

(defn long?
  "Returns `true` if `x` is of type `java.lang.Long`."
  [x] (instance? java.lang.Long x))

(defn bigint?
  "Returns `true` if `x` is of type `clojure.lang.BigInt`."
  [x] (instance? clojure.lang.BigInt x))

(defn bigdec?
  "Returns `true` if `x` is of type `java.math.BigDecimal`."
  [x] (instance? java.math.BigDecimal x))

(defn instant?
  "Returns `true` if `x` is of type `java.util.Date`."
  [x] (instance? java.util.Date x))

(defn uuid?
  "Returns `true` if `x` is of type `java.util.UUID`."
  [x] (instance? java.util.UUID x))

(defn uri?
  "Returns `true` if `x` is of type `java.net.URI`."
  [x] (instance? java.net.URI x))

(defn bytes?
  "Returns `true` if `x` is a primitive `byte` array."
  [x] (= (Class/forName "[B")
         (.getClass x)))

(defn type-checker
  "Returns the checking function associated with keyword `k`

     (type-checker :string)
     ;=> #'clojure.core/string?

     (type-checker :bytes)
     ;=> #'adi.utils/bytes?
   "
  [k]
  (resolve (symbol (str (name k) "?"))))

 ;; ## Misc Methods

(defn error
  "Throws an error when called. "
  ([e] (throw (Exception. (str e))))
  ([e & more]
     (throw (Exception. (apply str e more)))))

(defmacro suppress [& body]
  `(try ~@body (catch Throwable ~'t)))

(defn func-map
  "Returns a hash-map `m`, with the the values of `m` being
   the items within the collection and keys of `m` constructed
   by mapping `f` to `coll`. This is used to turn a collection
   into a lookup for better search performance.

    (funcmap :id [{:id 1 :val 1} {:id 2 :val 2}])
    ;=> {1 {:id 1 :val 1} 2 {:id 2 :val 2}}
   "
  [f coll] (zipmap (map f coll) coll))

(defn remove-repeats
  "Returns a vector of the items in `coll` for which `(f item)` is unique
   for sequential `item`'s in `coll`.

    (remove-repeats [1 1 2 2 3 3 4 5 6])
    ;=> [1 2 3 4 5 6]

    (remove-repeats even? [2 4 6 1 3 5])
    ;=> [2 1]
   "
  ([coll] (remove-repeats identity coll))
  ([f coll] (remove-repeats f coll [] nil))
  ([f coll output last]
     (if-let [v (first coll)]
       (cond (and last (= (f last) (f v)))
             (recur f (next coll) output last)
             :else (recur f (next coll) (conj output v) v))
       output)))

(defn replace-walk
  "Replaces all values in coll with the replacements defined as a lookup

    (replace-walk {:a 1 :b {:c 1}} {1 2})
    => {:a 2 :b {:c 2}}
  "
  [coll rep]
  (cond (vector? coll)   (mapv #(replace-walk % rep) coll)
        (list? coll)     (apply list (map #(replace-walk % rep) coll))
        (hash-set? coll) (set (map #(replace-walk % rep) coll))
        (hash-map? coll) (zipmap (keys coll)
                               (map #(replace-walk % rep) (vals coll)))
        (rep coll) (rep coll)
        :else coll))

(defn group-bys
  "Returns a map of the elements of coll keyed by the result of
  f on each element. The value at each key will be a set of the
  corresponding elements, in the order they appeared in coll."
  [f coll]
  (persistent!
   (reduce
    (fn [ret x]
      (let [k (f x)]
        (assoc! ret k (conj (get ret k #{}) x))))
    (transient {}) coll)))

(defn dissoc-in
  "Dissociates a key in a nested associative structure `m`, where `[k & ks]` is a
  sequence of keys. When `keep` is true, nested empty levels will not be removed.

    (dissoc-in {:a {:b {:c 3}}} [:a :b :c])
    ;=> {}

    (dissoc-in {:a {:b {:c 3}}} [:a :b :c] true)
    ;=> {:a {:b {}}}
  "
  ([m [k & ks]]
     (if-not ks
       (dissoc m k)
       (let [nm (dissoc-in (m k) ks)]
         (cond (empty? nm) (dissoc m k)
               :else (assoc m k nm)))))

  ([m [k & ks] keep]
     (if-not ks
       (dissoc m k)
       (assoc m k (dissoc-in (m k) ks keep)))))

(defn keys-nested
  "Return the set of all nested keys in `m`.

    (keys-in {:a {:b 1 :c {:d 1}}})
    ;=> #{:a :b :c :d})"
  ([m] (keys-nested m #{}))
  ([m ks]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (set/union
              (keys-nested (next m) (conj ks k))
              (keys-nested v))

             :else (recur (next m) (conj ks k)))
       ks)))

(defn dissoc-nested
   "Returns `m` without all nested keys in `ks`.

    (dissoc-nested {:a {:b 1 :c {:b 1}}} [:b])
    ;=> {:a {:c {}}}"

  ([m ks] (dissoc-nested m (set ks) {}))
  ([m ks output]
    (if-let [[k v] (first m)]
      (cond (ks k)
            (recur (next m) ks output)

            (hash-map? v)
            (recur (next m) ks
              (assoc output k (dissoc-nested v ks)))
            :else
            (recur (next m) ks (assoc output k v)))
      output)))

(defn diff-nested
  "Returns any nested values in `m1` that are different to those in `m2`.

    (diff-nested {:a {:b 1}}
                 {:a {:b 1 :c 1}})
    ;=> {}

    (diff-nested {:a {:b 1 :c 1}}
                 {:a {:b 1}})
    ;=> {:a {:c 1}}"
  ([m1 m2] (diff-nested m1 m2 {}))
  ([m1 m2 output]
     (if-let [[k v] (first m1)]
       (cond (nil? (k m2))
             (recur (dissoc m1 k) m2 (assoc output k v))

             (and (hash-map? v) (hash-map? (k m2)))
             (let [sub (diff-nested v (k m2))]
               (if (empty? sub)
                 (recur (dissoc m1 k) m2 output)
                 (recur (dissoc m1 k) m2 (assoc output k sub))))

             (not= v (k m2))
             (recur (dissoc m1 k) m2 (assoc output k v))

             :else
             (recur (dissoc m1 k) m2 output))
       output)))

(defn merge-nested
  "Merges all nested values in `m1`, `m2` and `ms` if there are more.
   nested values of maps on the right will replace those on the left if
   the keys are the same.

    (merge-nested {:a {:b {:c 3}}} {:a {:b 3}})
    ;=> {:a {:b 3}}

    (merge-nested {:a {:b {:c 1 :d 2}}}
                  {:a {:b {:c 3}}})
    ;=> {:a {:b {:c 3 :d 2}}})
  "
  ([m] m)
  ([m1 m2]
  (if-let [[k v] (first m2)]
    (cond (nil? (k m1))
          (recur (assoc m1 k v) (dissoc m2 k))

          (and (hash-map? v) (hash-map? (k m1)))
          (recur (assoc m1 k (merge-nested (k m1) v)) (dissoc m2 k))

          (not= v (k m1))
          (recur (assoc m1 k v) (dissoc m2 k))

          :else
          (recur m1 (dissoc m2 k)))
    m1))
  ([m1 m2 m3 & ms]
     (apply merge-nested (merge-nested m1 m2) m3 ms)))

(defn remove-nested
  "Returns a associative with nils and empty hash-maps removed.

    (remove-nested {:a {:b {:c {}}}})
    ;=> {}

    (remove-nested {:a {:b {:c {} :d 1}}})
    ;=> {:a {:b {:d 1}}}
  "
  ([m] (remove-nested m (constantly false) {}))
  ([m pred] (remove-nested m pred {}))
  ([m pred output]
     (if-let [[k v] (first m)]
       (cond (or (nil? v) (suppress (pred v)))
             (recur (dissoc m k) pred output)

             (hash-map? v)
             (let [rmm (remove-nested v pred)]
               (if (empty? rmm)
                 (recur (dissoc m k) pred output)
                 (recur (dissoc m k) pred (assoc output k rmm))))

             :else
             (recur (dissoc m k) pred (assoc output k v)))
       output)))

;; ## Keyword Methods

(defn keyword-str
  "Returns the string representation of the keyword
   without the colon.

    (keyword-str :hello/there)
    ;=> \"hello/there\"
  "
  [k]
  (if (nil? k) "" (#'st/replace-first-char (str k) \: "")))

(defn keyword-join
  "Merges a sequence of keywords into one.

    (keyword-join [:hello :there])
    ;=> :hello/there

    (keyword-join [:a :b :c :d])
    ;=> :a/b/c/d)"
  ([ks] (keyword-join ks "/"))
  ([ks sep]
     (if (empty? ks) nil
         (->> (filter identity ks)
              (map keyword-str)
              (st/join sep)
              keyword))))

(defn keyword-split
  "The opposite of `keyword-join`. Splits a keyword
   by the `/` character into a vector of keys.

    (keyword-split :hello/there)
    ;=> [:hello :there]

    (keyword-split :a/b/c/d)
    ;=> [:a :b :c :d]
  "
  ([k] (keyword-split k #"/"))
  ([k re]
     (if (nil? k) []
         (mapv keyword (st/split (keyword-str k) re)))))

(defn keyword-contains?
  "Returns `true` if the first part of `k` contains `subk`

    (keyword-contains? :a :a)
    ;=> true

    (keyword-contains? :a/b/c :a/b)
    ;=> true
  "
  [k subk]
  (or (= k subk)
      (starts-with (keyword-str k)
                   (str (keyword-str subk) "/"))))

(defn keyword-nsvec
  "Returns the namespace vector of keyword `k`.

    (keyword-nsvec :hello/there)
    ;=> [:hello]

    (keyword-nsvec :hello/there/again)
    ;=> [:hello :there]
  "
  [k]
  (or (butlast (keyword-split k)) []))

(defn keyword-nsvec?
  "Returns `true` if keyword `k` has the namespace vector `nsv`."
  [k nsv]
  (= nsv (keyword-nsvec k)))

(defn keyword-ns
  "Returns the namespace of `k`.

    (keyword-ns :hello/there)
    ;=> :hello

    (keyword-ns :hello/there/again)
    ;=> :hello/there
  "
  [k]
  (keyword-join (keyword-nsvec k)))

(defn keyword-ns?
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
  ([k ns] (if-let [tkns (keyword-ns k)]
            (= 0 (.indexOf (str k)
                 (str ns "/")))
            (nil? ns))))

(defn keyword-root
  "Returns the namespace root of `k`.

    (keyword-root :hello/there)
    ;=> :hello

    (keyword-root :hello/there/again)
    ;=> :hello
  "
  [k]
  (first (keyword-nsvec k)))

(defn keyword-root?
  "Returns `true` if keyword `k` has the namespace base `nsk`."
  [k nsk]
  (= nsk (keyword-root k)))

(defn keyword-stemvec
  "Returns the stem vector of `k`.

    (keyword-stemvec :hello/there)
    ;=> [:there]

    (keyword-stemvec :hello/there/again)
    ;=> [:there :again]
  "
  [k]
  (rest (keyword-split k)))

(defn keyword-stemvec?
  "Returns `true` if keyword `k` has the stem vector `kv`."
  [k kv]
  (= kv (keyword-stemvec k)))

(defn keyword-stem
  "Returns the steam of `k`.

    (keyword-stem :hello/there)
    ;=> :there

    (keyword-stem :hello/there/again)
    ;=> :there/again
  "
  [k]
  (keyword-join (keyword-stemvec k)))

(defn keyword-stem?
  "Returns `true` if keyword `k` has the stem `kst`."
  [k kst]
  (= kst (keyword-stem k)))

(defn keyword-val
  "Returns the keyword value of the `k`.

    (keyword-val :hello)
    ;=> :hello

    (keyword-val :hello/there)
    ;=> :there"
   [k]
  (last (keyword-split k)))

(defn keyword-val?
  "Returns `true` if the keyword value of `k` is equal
   to `z`."
  [k z]
  (= z (keyword-val k)))

(defn datmap-ns
  "Returns the set of keyword namespaces within `fm`.

    (datmap-ns {:hello/a 1 :hello/b 2
                      :there/a 3 :there/b 4})
    ;=> #{:hello :there}
  "
  [fm]
  (let [ks (keys fm)]
    (set (map keyword-ns ks))))

(defn datmap-ns?
  "Returns `true` if any key in `fm` has keyword namespace
  of `ns`.

    (datmap-ns? {:hello/a 1 :hello/b 2
                 :there/a 3 :there/b 4} :hello)
    ;=> true
  "
  [fm ns]
  (some #(keyword-ns? % ns) (keys fm)))

(defn datmap-keys
  "Returns the set of keys in `fm` that has keyword namespace
  of `ns`.

    (datmap-keys {:hello/a 1 :hello/b 2
                   :there/a 3 :there/b 4})
    ;=> {:there #{:there/a :there/b}, :hello #{:hello/b :hello/a}}
    (datmap-keys {:hello/a 1 :hello/b 2
                   :there/a 3 :there/b 4} :hello)
    ;=> #{:hello/a :hello/b})
  "
  ([fm] (let [ks (keys fm)]
          (group-bys #(keyword-ns %) ks)))
  ([fm ns]
     (let [ks (keys fm)]
       (->> ks
            (filter #(= ns (keyword-ns %)))
            set))))


(defn flatten-keys
  "Returns `m` with the first nest layer of keys flattened
   onto the root layer.

    (flatten-keys {:a {:b 2 :c 3} e: 4})
    ;=> {:a/b 2 :a/c 3 :e 4}

    (flatten-keys {:a {:b {:c 3 :d 4}
                           :e {:f 5 :g 6}}
                   :h {:i 7}
                   :j 8})
    ;=> {:a/b {:c 3 :d 4} :a/e {:f 5 :g 6} :h/i 7 :j 8})
  "
  ([m] (flatten-keys m {}))
  ([m output]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (let [ks      (->> (keys v)
                                (map #(keyword-join [k %])))
                   voutput (zipmap ks (vals v))]
               (recur (next m) (merge output voutput)))

              :else
              (recur (next m) (assoc output k v)))
       output)))

(defn flatten-keys-nested
  "Returns a single associative map with all of the nested
  keys of `m` flattened.

    (flatten-keys-nested {:a {:b {:c 3 :d 4}
                              :e {:f 5 :g 6}}
                          :h {:i 7}
                          :j 8 })
    ;=> {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7 :j 8}
  "
  ([m] (flatten-keys-nested m [] {}))
  ([m nskv output]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (->> output
                  (flatten-keys-nested (next m) nskv)
                  (recur v (conj nskv k)))

             (nil? v)
             (recur (next m) nskv output)

             :else
             (recur (next m)
                    nskv
                    (assoc output (keyword-join (conj nskv k)) v)))
       output))

  ([m keep] (flatten-keys-nested m keep [] {}))
  ([m keep nskv output]
     (if-let [[k v] (first m)]
       (cond (and (hash-map? v) (not (empty? v)))
             (->> output
                  (flatten-keys-nested (next m) keep nskv)
                  (recur v keep (conj nskv k)))

             (nil? v)
             (recur (next m) keep nskv output)

             :else
             (recur (next m) keep
                    nskv (assoc output (keyword-join (conj nskv k)) v)))
       output)))

(defn treeify-keys
  "Returns a nested map, expanding out the first
   level of keys into additional hash-maps.

    (treeify-keys {:a/b 2 :a/c 3})
    ;=> {:a {:b 2 :c 3}}

    (treeify-keys {:a/b {:e/f 1} :a/c {:g/h 1}})
    ;=> {:a {:b {:e/f 1}
             :c {:g/h 1}}}

  "
  ([m] (treeify-keys m {}))
  ([m output]
     (if-let [[k v] (first m)]
       (recur (rest m)
              (assoc-in output (keyword-split k) v))
       output)))

(defn treeify-keys-nested
  "Returns a nested map, expanding out all
   levels of keys into additional hash-maps.

    (treeify-keys-nested {:a/b 2 :a/c 3})
    ;=> {:a {:b 2 :c 3}}

    (treeify-keys-nested {:a/b {:e/f 1} :a/c {:g/h 1}})
    ;=> {:a {:b {:e {:f 1}}
             :c {:g {:h 1}}}}

  "
  [m]
  (let [kvs  (seq m)
        hm?  #(hash-map? (second %))
        ms   (filter hm? kvs)
        vs   (filter (complement hm?) kvs)
        outm (reduce (fn [m [k v]] (assoc-in m (keyword-split k)
                                            (treeify-keys-nested v)))
                    {} ms)]
    (reduce (fn [m [k v]] (assoc-in m (keyword-split k) v))
            outm vs)))



(defn nest-keys
  "Returns a map that takes `m` and extends all keys with the
   `nskv` vector. `ex` is the list of keys that are not extended.

    (nest-keys {:a 1 :b 2} [:hello :there])
    ;=> {:hello {:there {:a 1 :b 2}}}

    (nest-keys {:there 1 :b 2} [:hello] [:there])
    ;=> {:hello {:b 2} :there 1}
  "
  ([m nskv] (nest-keys m nskv []))
  ([m nskv ex]
    (let [e-map (select-keys m ex)
          x-map (apply dissoc m ex)]
      (merge e-map (if (empty? nskv)
                     x-map
                     (assoc-in {} nskv x-map))))))

(defn unnest-keys
  "The reverse of `nest-keys`. Takes `m` and returns a map
   with all keys with a `keyword-nsvec` of `nskv` being 'unnested'

    (unnest-keys {:hello/a 1
                     :hello/b 2
                     :there/a 3
                     :there/b 4} [:hello])
    ;=> {:a 1 :b 2
         :there {:c 3 :d 4}}

    (unnest-keys {:hello {:there {:a 1 :b 2}}
                     :again {:c 3 :d 4}} [:hello :there] [:+] )
    ;=> {:a 1 :b 2
         :+ {:again {:c 3 :d 4}}}
  "
  ([m nskv] (unnest-keys m nskv []))
  ([m nskv ex]
   (let [tm     (treeify-keys-nested m)
         c-map  (get-in tm nskv)
         x-map  (dissoc-in tm nskv)]
    (merge c-map (if (empty? ex)
                   x-map
                   (assoc-in {} ex x-map))))))

(defn eq-cmp
  [v1 v2 cmp]
  (cond (vector? cmp)
        (= (get-in v1 cmp) (get-in v2 cmp))

        :else
        (= (cmp v1) (cmp v2))))

(defn combine-obj
  "Looks for the value within the set `s` that is the same as `v` when
   `cmp` is applied to both.

    (h/combine-obj #{1 2 3} 2 identity)
    ;=> 2

    (h/combine-obj #{{:id 1}} {:id 1 :val 1} :id)
    ;=> {:id 1}"

  [s v cmp]
  (if-let [sv (first s)]
    (if (eq-cmp sv v cmp)
      sv
      (recur (next s) v cmp))))

(defn combine-to-set
  ""
  [s v cmp rd]
  (if-let [sv (combine-obj s v cmp)]
    (conj (disj s sv) (rd sv v))
    (conj s v)))

(defn combine-sets
  [s1 s2 cmp rd]
  (if-let [v (first s2)]
    (recur (combine-to-set s1 v cmp rd) (next s2) cmp rd)
    s1))

(defn combine-internal
  [s cmp rd]
  (if-not (hash-set? s) s
          (combine-sets #{} s cmp rd)))

(defn combine
  ([v1 v2]
     (cond (nil? v2) v1
           (nil? v1) v2
           (hash-set? v1)
           (cond (hash-set? v2)
                 (set/union v1 v2)
                 :else (conj v1 v2))
           :else
           (cond (hash-set? v2)
                 (conj v2 v1)

                 (= v1 v2) v1
                 :else #{v1 v2})))
  ([v1 v2 cmp rd]
     (-> (cond (nil? v2) v1
               (nil? v1) v2
               (hash-set? v1)
               (cond (hash-set? v2)
                     (combine-sets v1 v2 cmp rd)

                     :else (combine-to-set v1 v2 cmp rd))
               :else
               (cond (hash-set? v2)
                     (combine-to-set v2 v1 cmp rd)

                     (and (eq-cmp v1 v2 cmp)
                          (not= nil (cmp v1)))
                     (rd v1 v2)

                     (= v1 v2) v1
                     :else #{v1 v2}))
         (combine-internal cmp rd))))

(defn merges
  ([m1 m2] (merges m1 m2 {}))
  ([m1 m2 output]
     (if-let [[k v] (first m2)]
       (recur (dissoc m1 k) (rest m2)
              (assoc output k (combine (m1 k) v)))
       (merge m1 output)))
  ([m1 m2 cmp rd] (merges m1 m2 cmp rd {}))
  ([m1 m2 cmp rd output]
     (if-let [[k v] (first m2)]
       (recur (dissoc m1 k) (rest m2) cmp rd
              (assoc output k (combine (m1 k) v cmp rd)))
       (merge m1 output))))

(defn merges-in
  ([m1 m2] (merges-in m1 m2 {}))
  ([m1 m2 output]
     (if-let [[k v2] (first m2)]
       (let [v1 (m1 k)]
         (cond (not (and (hash-map? v1) (hash-map? v2)))
               (recur (dissoc m1 k) (rest m2)
                      (assoc output k (combine v1 v2)))
               :else
               (recur (dissoc m1 k) (rest m2)
                            (assoc output k (merges-in v1 v2)))))
       (merge m1 output)))
  ([m1 m2 cmp rd] (merges-in m1 m2 cmp rd {}))
  ([m1 m2 cmp rd output]
     (if-let [[k v2] (first m2)]
       (let [v1 (m1 k)]
         (cond (not (and (hash-map? v1) (hash-map? v2)))
               (recur (dissoc m1 k) (rest m2) cmp rd
                      (assoc output k (combine v1 v2 cmp rd)))
               :else
               (recur (dissoc m1 k) (rest m2) cmp rd
                            (assoc output k (merges-in v1 v2 cmp rd)))))
       (merge m1 output))))

(defn merges-in*
  ([m1 m2] (merges-in* m1 m2 hash-map? combine {}))
  ([m1 m2 cmp] (merges-in* m1 m2 cmp combine {}))
  ([m1 m2 cmp rd] (merges-in* m1 m2 cmp rd {}))
  ([m1 m2 cmp rd output]
     (cond (hash-map? m1)
           (if-let [[k v2] (first m2)]
             (let [v1 (m1 k)
                   nm1 (dissoc m1 k)
                   nm2 (rest m2)]
               (cond (and (hash-map? v1) (hash-map? v2))
                     (recur nm1 nm2 cmp rd
                            (assoc output k (merges-in* v1 v2 cmp rd)))

                     (or (hash-set? v1) (hash-set? v2))
                     (recur nm1 nm2 cmp rd
                            (assoc output k
                                   (combine v1 v2 cmp
                                            #(merges-in* %1 %2 cmp rd))))
                     :else
                     (recur nm1 nm2 cmp rd
                            (assoc output k (rd v1 v2)))))
             (merge m1 output))

           :else
           (combine m1 m2))))

(defn assocs
  ([m k v]
     (let [z (get m k)]
       (cond (nil? z) (assoc m k v)
             :else
             (assoc m k (combine z v)))))

  ([m k v cmp rd]
     (let [z (get m k)]
       (cond (nil? z) (assoc m k v)
             :else
             (assoc m k (combine z v cmp rd))))))

(defn decombine
  [s1 v]
  (cond (hash-set? v)
        (set/difference s1 v)

        (ifn? v)
        (set (filter (complement v) s1))

        :else (disj s1 v)))

(defn dissocs-vec
  [m [k v]]
  (let [z (get m k)]
    (cond (hash-set? z)
          (let [hs (decombine z v)]
            (if (empty? hs)
              (dissoc m k)
              (assoc m k hs)))
          :else
          (if (or (= v z)
                  (and (or (hash-set? v)
                           (ifn? v))
                       (v z)))
            (dissoc m k) m))))

(defn dissocs
  ([m k]
     (cond (vector? k)
           (dissocs-vec m k)
           :else (dissoc m k)))
  ([m k1 k2 & ks]
     (apply dissocs (dissocs m k1) k2 ks)))

(defn eq-cmp
  [v1 v2 cmp]
  (cond (vector? cmp)
        (= (get-in v1 cmp) (get-in v2 cmp))

        :else
        (= (cmp v1) (cmp v2))))

(defn eq-chk-fn [v chk]
  (or (= v chk)
      (and (ifn? chk) (chk v))))

(defn eq-chk [v chk cmp]
  (cond (vector? cmp)
        (eq-chk-fn (get-in v cmp) chk)
        :else
        (eq-chk-fn (cmp v) chk)))

(defn assocs-in-ok? [m pred]
  (cond (vector? pred)
        (let [[cmp chk] pred]
          (eq-chk m chk cmp))
        (ifn? pred) (pred m)))

(declare assocs-in
         assocs-in-keyword assocs-in-filtered)

(defn assocs-in
  [m [k & ks :as all-ks] v]
  (cond (nil? ks)
        (cond (vector? k) (error "cannot allow vector-form on last key " k)
              (or (nil? m) (hash-map? m)) (assocs m k v)
              (nil? k) (combine m v)
              :else (error m " is not an associative map"))

        (or (nil? m) (hash-map? m))
        (cond (vector? k) (assocs-in-filtered m all-ks v)
              :else (assocs-in-keyword m all-ks v))
        :else (error m " is required to be a map")))

(defn assocs-in-keyword
  [m [k & ks :as all-ks] v]
  (let [val (get m k)]
    (cond (hash-set? val)
          (assoc m k (set (map #(assocs-in-keyword % ks v) val)))
          :else (assoc m k (assocs-in val ks v)))))

(defn assocs-in-filtered
  [m [[k pred] & ks :as all-ks] v]
  (let [subm (get m k)]
    (cond (nil? subm) m

          (and (hash-set? subm) (every? hash-map? subm))
          (let [ori-set (set (filter #(assocs-in-ok? % pred) subm))
                new-set (set (map #(assocs-in % ks v) ori-set))]
            (assoc m k (-> subm
                           (set/difference ori-set)
                           (set/union new-set))))

          (hash-map? subm)
          (if (assocs-in-ok? subm pred)
            (assoc m k (assocs-in subm ks v))
            m)

          :else (error subm "needs to be hash-map or hash-set"))))

(defn gets-in
  [m [k & ks :as all-ks]])

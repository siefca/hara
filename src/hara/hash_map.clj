(ns hara.hash-map
  (:use hara.common)
  (:require [clojure.string :as st]
            [clojure.set :as set])
  (:refer-clojure :exclude [send]))

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
   keys of `m` flattened. If `keep` is added, it preserves all the
   empty sets.

    (flatten-keys-nested {:a {:b {:c 3 :d 4}
                              :e {:f 5 :g 6}}
                          :h {:i {}}})
    ;=> {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6}

    (flatten-keys-nested {:a {:b {:c 3 :d 4}
                              :e {:f 5 :g 6}}
                          :h {:i {}}}
                          true)
    ;=> {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i {}}
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

(defn combine-obj
  "Looks for the value within the set `s` that matches `v` when
   `cmp` is applied to both.

    (combine-obj #{1 2 3} 2 identity)
    ;=> 2

    (combine-obj #{{:id 1}} {:id 1 :val 1} :id)
    ;=> {:id 1}
  "
  [s v cmp]
  (if-let [sv (first s)]
    (if (eq-cmp sv v cmp)
      sv
      (recur (next s) v cmp))))

(defn combine-to-set
  "Returns `s` with either v added or combined to an existing set member.

    (combine-to-set #{{:id 1 :a 1} {:id 2}}
                    {:id 1 :b 1}
                    :id merge)
    ;=> #{{:id 1 :a 1 :b} {:id 2}}
  "
  [s v cmp rd]
  (if-let [sv (combine-obj s v cmp)]
    (conj (disj s sv) (rd sv v))
    (conj s v)))

(defn combine-sets
  "Returns the combined set of `s1` and `s2`.

    (combine-sets #{{:id 1} {:id 2}}
                  #{{:id 1 :val 1} {:id 2 :val 2}}
                  :id merge)
    ;=> #{{:id 1 :val 1} {:id 2 :val 2}}
  "
  [s1 s2 cmp rd]
  (if-let [v (first s2)]
    (recur (combine-to-set s1 v cmp rd) (next s2) cmp rd)
    s1))

(defn combine-internal
  "Combines elements in `s` using rules defined by `cmp` and `rd`.

    (combine-internal #{{:id 1} {:id 2} {:id 1 :val 1} {:id 2 :val 2}}
                      :id merge)
    ;=> #{{:id 1 :val 1} {:id 2 :val 2}}
  "
  [s cmp rd]
  (if-not (hash-set? s) s
          (combine-sets #{} s cmp rd)))

(defn combine
  "Generic function that looks at `v1` and `v2`, which can be either
   values or sets of values and merges them into a new set.

    (combine 1 2) ;=> #{1 2}

    (combine #{1} 1) ;=> #{1}

    (combine #{{:id 1} {:id 2}}
             #{{:id 1 :val 1} {:id 2 :val 2}}
             :id merge)
    ;=> #{{:id 1 :val 1} {:id 2 :val 2}}

   "
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

                     (eq-cmp v1 v2 cmp)
                     (rd v1 v2)

                     (= v1 v2) v1
                     :else #{v1 v2}))
         (combine-internal cmp rd))))

(defn decombine
  "Returns `v` without every single member of `dv`.

    (decombine 1 1) => nil

    (decombine 1 2) => 1

    (decombine #{1} 1) => nil

    (decombine #{1 2 3 4} #{1 2}) => #{3 4}

    (decombine #{1 2 3 4} even?) => #{1 3}
  "
  [v dv]
  (cond (hash-set? v)
        (let [res (cond (hash-set? dv)
                        (set/difference v dv)

                        (ifn? dv)
                        (set (filter (complement dv) v))

                        :else (disj v dv))]
          (if-not (empty? res) res))
        :else
        (if-not (eq-chk v dv) v)))

(defn merges
  "Like `merge` but works across sets and will also
   combine duplicate key/value pairs together into sets of values.

    (merges {:a 1} {:a 2}) ;=> {:a #{1 2}}

    (merges {:a #{{:id 1 :val 1}}}
            {:a {:id 1 :val 2}}
            :id merges)
    ;=> {:a #{{:id 1 :val #{1 2}}}}

  "
  ([m1 m2] (merges m1 m2 identity combine {}))
  ([m1 m2 cmp] (merges m1 m2 cmp combine {}))
  ([m1 m2 cmp rd] (merges m1 m2 cmp rd {}))
  ([m1 m2 cmp rd output]
     (if-let [[k v] (first m2)]
       (recur (dissoc m1 k) (rest m2) cmp rd
              (assoc output k (combine (m1 k) v cmp rd)))
       (merge m1 output))))

(defn merges-in
  "Like `merges` but works on nested maps

    (merges-in {:a {:b 1}} {:a {:b 2}})
    ;=> {:a {:b #{1 2}}}

    (merges-in {:a #{{:foo #{{:bar #{{:baz 1}}}}}}}
               {:a #{{:foo #{{:bar #{{:baz 2}}}}}}}
               hash-map?
               merges-in)
    => {:a #{{:foo #{{:bar #{{:baz 2}}}
                     {:bar #{{:baz 1}}}}}}}
  "
  ([m1 m2] (merges-in m1 m2 identity combine {}))
  ([m1 m2 cmp] (merges-in m1 m2 cmp combine {}))
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
  "Like `merges-in but can recursively merge nested sets.

    h/merges-in* {:a #{{:id 1 :foo
                              #{{:id 2 :bar
                                       #{{:id 3 :baz 1}}}}}}}
                 {:a #{{:id 1 :foo
                              #{{:id 2 :bar
                                       #{{:id 3 :baz 2}}}}}}}
                 :id)
    ;=> {:a #{{:id 1 :foo
                     #{{:id 2 :bar
                              #{{:id 3 :baz #{1 2}}}}}}}}
"
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
  "Similar to `assoc` but conditions of association is specified
  through `cmp` (default: `identity`) and well as merging specified
  through `rd` (default: `combine`).

    (assocs {:a #{1}} :a #{2 3 4}) ;=> {:a #{1 2 3 4}}

    (assocs {:a {:id 1}} :a {:id 1 :val 1} :id merge)
    ;=> {:a {:val 1, :id 1}}

    (assocs {:a #{{:id 1 :val 2}
                  {:id 1 :val 3}}} :a {:id 1 :val 4} :id merges)
    ;=> {:a #{{:id 1 :val #{2 3 4}}}})

  "
  ([m k v] (assocs m k v identity combine))
  ([m k v cmp rd]
     (let [z (get m k)]
       (cond (nil? z) (assoc m k v)
             :else
             (assoc m k (combine z v cmp rd))))))

(defn dissocs
  "Similar to `dissoc` but allows dissassociation of sets of values from a map.

    (dissocs {:a 1} :a) ;=> {}

    (dissocs {:a #{1 2}} [:a #{0 1}]) ;=> {:a #{2}}

    (dissocs {:a #{1 2}} [:a #{1 2}]) ;=> {}
  "
  [m k]
  (cond (vector? k)
        (let [[k v] k
              z (get m k)
              res (decombine z v)]
          (if (nil? res)
            (dissoc m k)
            (assoc m k res)))
        :else
        (dissoc m k)))

(defn gets
  "Returns the associated values either specified by a key or a key and predicate pair.

    (gets {:a 1} :a) => 1

    (gets {:a #{0 1}} [:a zero?]) => #{0}

    (gets {:a #{{:b 1} {}}} [:a :b]) => #{{:b 1}}
  "
  [m k]
  (if-not (vector? k) (get m k)
          (let [[k pred] k
                val (get m k)]
            (if-not (hash-set? val) val
                    (-> (filter #(val-pred? % pred) val) set)))))

(declare gets-in gets-in-loop)

(defn gets-in
  "Similar in style to `get-in` with operations on sets. Returns a set of values.

    (gets-in {:a 1} [:a]) => #{1}

    (gets-in {:a 1} [:b]) => #{}

    (gets-in {:a #{{:b 1} {:b 2}}} [:a :b]) => #{1 2}
  "
  [m ks]
  (-> (gets-in-loop m ks) set (disj nil)))

(defn- gets-in-loop
  [m [k & ks :as all-ks]]
  (cond (nil? ks)
        (let [val (gets m k)]
          (cond (hash-set? val) val
                :else (list val)))
        :else
        (let [val (gets m k)]
          (cond (hash-set? val)
                (apply concat (map #(gets-in-loop % ks) val))
                :else (gets-in-loop val ks)))))

(declare assocs-in
         assocs-in-keyword assocs-in-filtered)

(defn assocs-in
  "Similar to assoc-in but with power of moving through sets

    (h/assocs-in {:a {:b 1}} [:a :b] 2)
    ;=> {:a {:b #{1 2}}}

    (h/assocs-in {:a #{{:b 1}}} [:a :b] 2)
    ;=> {:a #{{:b #{1 2}}}}

    (h/assocs-in {:a #{{:b {:id 1}} {:b {:id 2}}}}
                 [:a [:b [:id 1]] :c] 2)
    ;=> {:a #{{:b {:id 1 :c 2}} {:b {:id 2}}}}
  "
  ([m all-ks v] (assocs-in m all-ks v identity combine))
  ([m [k & ks :as all-ks] v cmp rd]
     (cond (nil? ks)
           (cond (vector? k) (error "cannot allow vector-form on last key " k)
                 (or (nil? m) (hash-map? m)) (assocs m k v cmp rd)
                 (nil? k) (combine m v cmp rd)
                 :else (error m " is not an associative map"))

           (or (nil? m) (hash-map? m))
           (cond (vector? k) (assocs-in-filtered m all-ks v cmp rd)
                 :else
                 (let [val (get m k)]
                   (cond (hash-set? val)
                         (assoc m k (set (map #(assocs-in % ks v cmp rd) val)))
                         :else (assoc m k (assocs-in val ks v cmp rd)))))
           :else (error m " is required to be a map"))))

(defn assocs-in-filtered
  ([m all-ks v] (assocs-in-filtered m all-ks v identity combine))
  ([m [[k pred] & ks :as all-ks] v cmp rd]
     (let [subm (get m k)]
       (cond (nil? subm) m

             (and (hash-set? subm) (every? hash-map? subm))
             (let [ori-set (set (filter #(val-pred? % pred) subm))
                   new-set (set (map #(assocs-in % ks v cmp rd) ori-set))]
               (assoc m k (-> subm
                              (set/difference ori-set)
                              (set/union new-set))))

             (hash-map? subm)
             (if (val-pred? subm pred)
               (assoc m k (assocs-in subm ks v cmp rd))
               m)

             :else (error subm "needs to be hash-map or hash-set")))))

(declare dissocs-in dissocs-in-filtered)

(defn dissocs-in
  "Similiar to `dissoc-in` but with sets manipulation.

    (dissocs-in {:a #{{:b 1 :c 1} {:b 2 :c 2}}}
                [:a :b])
    ;=> {:a #{{:c 1} {:c 2}}}

    (dissocs-in {:a #{{:b #{1 2 3} :c 1}
                      {:b #{1 2 3} :c 2}}}
                [[:a [:c 1]] [:b 1]])
    ;=> {:a #{{:b #{2 3} :c 1} {:b #{1 2 3} :c 2}}}
  "
  [m [k & ks :as all-ks]]
  (cond (nil? ks) (dissocs m k)

        (vector? k) (dissocs-in-filtered m all-ks)

        :else
        (let [val (get m k)]
          (cond (hash-set? val)
                (assoc m k (set (map #(dissocs-in % ks) val)))
                :else (assoc m k (dissocs-in m ks))))))

(defn dissocs-in-filtered
  ([m [[k pred] & ks :as all-ks]]
     (let [subm (get m k)]
       (cond (nil? subm) m
             (and (hash-set? subm) (every? hash-map? subm))
             (let [ori-set (set (filter #(val-pred? % pred) subm))
                   new-set (set (map #(dissocs-in % ks) ori-set))]
               (assoc m k (-> subm
                              (set/difference ori-set)
                              (set/union new-set))))

             (hash-map? subm)
             (if (val-pred? subm pred)
               (assoc m k (dissocs-in subm ks))
               m)

             :else (error subm "needs to be hash-map or hash-set")))))

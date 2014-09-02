(ns hara.sort.topological
  (:require [clojure.set :refer [difference union intersection]]))

(defn top-nodes
  "nodes that have no other nodes that are dependent on them
  (top-nodes {:a #{} :b #{:a}})
  => #{:b}"
  {:added "2.1"}
  [g]
  (let [nodes (set (keys g))
        dependent-nodes (apply union (vals g))]
    (difference nodes dependent-nodes)))

(defn topological-sort
  "sorts a directed graph into its dependency order

  (topological-sort {:a #{:b :c},
                     :b #{:d :e},
                     :c #{:e :f},
                     :d #{},
                     :e #{:f},
                     :f nil})
  => [:f :d :e :b :c :a]

  (topological-sort {:a #{:b},
                     :b #{:a}})
  => (throws Exception \"Graph Contains Circular Dependency: {:b #{:a}, :a #{:b}}\")"
  {:added "2.1"}
  ([g]
     (let [g (let [dependent-nodes (apply union (vals g))]
               (reduce #(if (get % %2) % (assoc % %2 #{})) g dependent-nodes))]
       (topological-sort g () (top-nodes g))))
  ([g l s]
     (cond (empty? s)
           (if (every? empty? (vals g))
             l
             (throw (Exception. (str "Graph Contains Circular Dependency: "
                                     (->> g
                                          (filter (fn [[k v]] (-> v empty? not)))
                                          (into {}))))))

           :else
           (let [[n s*] (if-let [item (first s)]
                          [item (difference s #{item})])
                 m (g n)
                 g* (reduce #(update-in % [n] difference #{%2}) g m)]
             (recur g* (cons n l) (union s* (intersection (top-nodes g*) m)))))))

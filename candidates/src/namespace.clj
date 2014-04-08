
(defn ns-vars [ns]
  (vec (sort (keys (ns-publics ns)))))
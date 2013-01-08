(ns hara.prop)

;;(def v [1])
;;(dosync (alter v (fn [x] x)))

(defn set-cell "Set the value of a cell" [cell value]
  (send cell (constantly value)))

(defmacro run-propagator
  "Run a propagator, first collect the most recent values from all
cells associated with the propagator, then evaluate."
  [propagator]
  `(let [results# (apply ~propagator (map deref (:in-cells (meta ~propagator))))]
     (doseq [cell# (:out-cells (meta ~propagator))]
       (when (not (= @cell# results#))
         (set-cell cell# results#)))
     results#))

(defn add-neighbor "Add a neighbor to the given cell." [cell neighbor]
  (add-watch cell nil (fn [_ _ _ _] (future (run-propagator neighbor)))))

(defmacro defcell "Define a new cell." [name state]
  `(def ~name (agent ~state)))

(defmacro defpropagator "Define a new propagator."
  [name in-cells out-cells & body]
  `(let [v# (defn ~(vary-meta name assoc :in-cells in-cells :out-cells out-cells)
              ~in-cells ~@body)]
     (doseq [cell# ~in-cells] (add-neighbor cell# ~name))
     v#))

(defcell guess 1)
(defcell x 9)
(defcell done false)
(defcell margin 0.1)

(defn abs [x] (if (< 0 x) x (- x)))
  ;;(abs -4)

  ;; check if we're close enough to a solution to cease improving

(defpropagator enough [x guess] [done]
  (Thread/sleep 1000) ; sleep to allow observation of incremental calculation
  (if (< (abs (- (* guess guess) x)) @margin) true false))

  ;; incrementally improve our guess

(defpropagator heron [x done guess] [guess]
  (Thread/sleep 1000)
  (if done
    guess
    (/ (+ guess (/ x guess)) 2.0)))

(pprint heron)

(comment           ; after building the system
  (set-cell x 89)  ; update the value of x
  (run-propagator enough)
  (deref guess)    ; immediately begin observing improvements in guess
  (deref guess)
  (deref x)
  (deref done))

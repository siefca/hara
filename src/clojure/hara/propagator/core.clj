(ns hara.propagator.core)

(def alerted-propagators (ref #{}))

(defn alert-propagators
  "Schedule a collection of propagators to execute at the next run"
  [coll]
  (dosync (commute alerted-propagators
                   (fn [prior] (apply conj prior coll)))))

(defn nothing? [item] (= item ::nothing))
(defn contradictory? [item] (= item ::contradiction))
(defn- raise-inconsistency [] (throw (Exception. "Inconsistent fact!")))

(defn- content-merge [pv nv]
  (cond
   (nothing? pv) nv
   (nothing? nv) pv
   (not (= pv nv)) ::contradiction
   :else pv))

(defprotocol CellProtocol
  (new-neighbor [cell neighbor])
  (add-content [cell content])
  (content [cell])
  (remove-content [cell])
  (neighbors [cell]))


(defn cell-set-content [state content]
  (dosync (alter state assoc :content content)))

(defn cell-add-neighbor [state neighbor]
  (dosync (alter state update-in [:neighbors] conj neighbor)))


(defrecord Cell [state]
  CellProtocol
  (new-neighbor [cell neighbor]
    (if-not (some #{neighbor} (:neighbors @cell))
      (cell-add-neighbor state neighbor)))
  (add-content [cell content]
    (let [pv  (content cell)
          ans (content-merge pv content)]
      (cond
       (= ans pv) cell
       (contradictory? cell) (raise-inconsistency)
       :else (do
               (cell-set-content state ans)
               cell))))
  (content [cell] (:content @state))
  (remove-content [cell] (cell-set-content state ::nothing))
  (neighbors [cell] (:neighbors @state)))

(defn items-before
  [coll sentinel]
  (let [pred (partial not= sentinel)]
    (take-while pred coll)))

(defn- cell-neighbor-watch
  [_ _ {[old-head & _ :as old-neighbors] :neighbors} {new-neighbors :neighbors}]
  (if (not= old-neighbors new-neighbors)
    (alert-propagators (items-before new-neighbors old-head))))

(defn- cell-content-watch
  [_ _ {old-content :content} {neighbors :neighbors, new-content :content}]
  (if (and (not= old-content new-content) (not (empty? neighbors)))
    (alert-propagators neighbors)))

(defn make-cell
  ([] (make-cell ::nothing))
  ([content]
      (Cell.
       (doto (ref {:neighbors [] :content content})
         (add-watch :neighbors cell-neighbor-watch)
         (add-watch :content cell-content-watch)))))

(def x (make-cell))
(println x)

(comment
  (defn new-neighbor [neighbors nn]
    (if (not (neighbors nn))
      (conj neighbors nn)))

  (def add-content [neighbors content increment]
    (cond (nothing? increment) :ok
          (nothing? content)
          (dosync (ref-set cont'))))

  (def make-cell []
    (let [neighbors #{}
          content nothing])))

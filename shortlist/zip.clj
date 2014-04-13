(ns hara.zip
  (:require [hara.maybe :refer [if-let when-let comp]])
  (:refer-clojure :exclude [comp if-let when-let replace remove next]))

(defrecord ZipperPath [l r ppath pnodes changed?])

(defrecord ZipperLocation [branch? children make-node node path])

(defn zipper
  "Creates a new zipper structure.

  branch? is a fn that, given a node, returns true if can have
  children, even if it currently doesn't.

  children is a fn that, given a branch node, returns a seq of its
  children.

  make-node is a fn that, given an existing node and a seq of
  children, returns a new branch node with the supplied children.
  root is the root node."
  {:added "1.0"}
  [branch? children make-node root]
  (ZipperLocation. branch? children make-node root nil))

(defn node
  "Returns the node at loc"
  [^ZipperLocation loc]
  (if loc
    (.node loc)))

(defn branch?
  "Returns true if the node at loc is a branch"
  [^ZipperLocation loc]
  (if loc
    ((.branch? loc) (.node loc))))

(defn children
  "Returns a seq of the children of node at loc, which must be a branch"
  [^ZipperLocation loc]
  (if loc
    ((.children loc) (.node loc))))

(defn make-node
  "Returns a new branch node, given an existing node and new children.
  The loc is only used to supply the constructor."
  [^ZipperLocation loc node children]
  (if loc
    ((.make-node loc) node children)))

(defn path
  "Returns a seq of nodes leading to this loc"
  [^ZipperLocation loc]
  (if-let [_  loc
           p (.path loc)]
    (.pnodes ^ZipperPath p)))

(defn lefts
  "Returns a seq of the left siblings of this loc"
  [^ZipperLocation loc]
  (if-let [_ loc
           p (.path loc)]
    (seq (reverse (.l ^ZipperPath p)))))

(defn rights
  "Returns a seq of the right siblings of this loc"
  [^ZipperLocation loc]
  (if-let [_ loc
           p (.path loc)]
    (.r ^ZipperPath p)))

(defn down
  "Returns the loc of the leftmost child of the node at this loc,
  or nil if no children"
  [^ZipperLocation loc]
  (when-let [_  loc
             _  (branch? loc)
             cs (children loc)
             node (.node loc)
             path ^ZipperPath (.path loc)]
    (ZipperLocation.
     (.branch? loc)
     (.children loc)
     (.make-node loc)
     (first cs)
     (ZipperPath. '()
                  (clojure.core/next cs)
                  path
                  (if path (conj (.pnodes path) node) [node]) nil))))

(defn up
  "Returns the loc of the parent of the node at this loc, or nil if at the top"
  [^ZipperLocation loc]
  (when-let [_ loc
             node (.node loc)
             path ^ZipperPath (.path loc)
             pnodes (and path (.pnodes path))
             pnode  (peek pnodes)]
    (if (.changed? path)
      (ZipperLocation.
       (.branch? loc)
       (.children loc)
       (.make-node loc)
       (make-node loc pnode (concat (reverse (.l path)) (cons node (.r path))))
       (if-let [ppath (.ppath path)] (assoc ppath :changed? true)))
      (ZipperLocation.
       (.branch? loc)
       (.children loc)
       (.make-node loc)
       pnode
       (.ppath path)))))

(defn root
  "zips all the way up and returns the root node, reflecting any changes."
  [^ZipperLocation loc]
  (if (identical? ::end (.path loc))
    (.node loc)
    (let [p (up loc)]
      (if p
        (recur p)
        (.node loc)))))

(defn right
  "Returns the loc of the right sibling of the node at this loc, or nil"
  [^ZipperLocation loc]
  (when-let [_   loc
             path ^ZipperPath (.path loc)
             r (and path (.r path))]
    (ZipperLocation.
     (.branch? loc)
     (.children loc)
     (.make-node loc)
     (first r)
     (assoc path :l (conj (.l path) (.node loc)) :r (clojure.core/next r)))))

(defn rightmost
  "Returns the loc of the rightmost sibling of the node at this loc, or self"
  [^ZipperLocation loc]
  (if loc
    (if-let [path ^ZipperPath (.path loc)
             r   (.r path)]
      (ZipperLocation.
       (.branch? loc)
       (.children loc)
       (.make-node loc)
       (last r)
       (assoc path :l (apply conj (.l path) (.node loc) (butlast r)) :r nil))
      loc)))

(defn left
  "Returns the loc of the left sibling of the node at this loc, or nil"
  [^ZipperLocation loc]
  (when-let [_    loc
             path ^ZipperPath (.path loc)
             l    (seq (.l path))]
    (ZipperLocation.
     (.branch? loc)
     (.children loc)
     (.make-node loc)
     (peek (.l path))
     (assoc path :l (pop (.l path)) :r (cons (.node loc) (.r path))))))

(defn leftmost
  "Returns the loc of the leftmost sibling of the node at this loc, or self"
  [^ZipperLocation loc]
  (if loc
    (if-let [path ^ZipperPath (.path loc)
             l (seq (.l path))]
      (ZipperLocation.
       (.branch? loc)
       (.children loc)
       (.make-node loc)
       (peek (.l path))
       (assoc path :l [] :r (concat (clojure.core/next (reverse (.l path))) [(.node loc)] (.r path))))
      loc)))

(defn insert-left
  "Inserts the item as the left sibling of the node at this loc, without moving"
  [^ZipperLocation loc item]
  (if-let [path ^ZipperPath (.path loc)]
    (ZipperLocation.
      (.branch? loc)
      (.children loc)
      (.make-node loc)
      (.node loc)
      (assoc path :l (conj (.l path) item) :changed? true))
    (throw (new Exception "Insert at top"))))

(defn insert-right
  "Inserts the item as the right sibling of the node at this loc, without moving"
  [^ZipperLocation loc item]
  (if-let [path ^ZipperPath (.path loc)]
    (ZipperLocation.
      (.branch? loc)
      (.children loc)
      (.make-node loc)
      (.node loc)
      (assoc path :r (cons item (.r path)) :changed? true))
    (throw (new Exception "Insert at top"))))

(defn replace
  "Replaces the node at this loc, without moving"
  [^ZipperLocation loc node]
  (ZipperLocation.
    (.branch? loc)
    (.children loc)
    (.make-node loc)
    node
    (if-let [path (.path loc)] (assoc path :changed? true))))

(defn insert-child
  "Inserts the item as the leftmost child of the node at this loc, without moving"
  [^ZipperLocation loc item]
  (replace loc (make-node loc (.node loc) (cons item (children loc)))))

(defn append-child
  "Inserts the item as the rightmost child of the node at this loc, without moving"
  [^ZipperLocation loc item]
  (replace loc (make-node loc (.node loc) (concat (children loc) [item]))))

(defn next
  "Moves to the next loc in the hierarchy, depth-first. When reaching
  the end, returns a distinguished loc detectable via end?. If already
  at the end, stays there."
  [^ZipperLocation loc]
  (if-let [_    loc
           path (.path loc)]
    (if (identical? ::end path)
      loc
      (or (and (branch? loc) (down loc))
          (right loc)
          (loop [p loc]
            (if-let [nloc  (-> p up right)]
              (recur nloc)
              (ZipperLocation. (.branch? loc) (.children loc) (.make-node loc) (.node p) ::end)))))))

(defn prev
  "Moves to the previous loc in the hierarchy, depth-first. If already at the root, returns nil."
  [loc]
  (if-let [lloc (left loc)]
    (loop [loc lloc]
      (if-let [child (and (branch? loc) (down loc))]
        (recur (rightmost child))
        loc))
    (up loc)))

(defn end?
  "Returns true if loc represents the end of a depth-first walk"
  [^ZipperLocation loc]
  (identical? ::end (.path loc)))

(defn remove
  "Removes the node at loc, returning the loc that would have preceded it in a depth-first walk."
  [^ZipperLocation loc]
  (if-let [path ^ZipperPath (.path loc)]
    (if (pos? (count (.l path)))
      (loop [loc (ZipperLocation.
                   (.branch? loc)
                   (.children loc)
                   (.make-node loc)
                   (peek (.l path))
                   (assoc path :l (pop (.l path)) :changed? true))]
        (if-let [child (and (branch? loc) (down loc))]
          (recur (rightmost child))
          loc))
      (ZipperLocation.
        (.branch? loc)
        (.children loc)
        (.make-node loc)
        (make-node loc (peek (.pnodes path)) (.r path))
        (if-let [ppath (.ppath path)] (and ppath (assoc ppath :changed? true)))))
    (throw (new Exception "Remove at top"))))

(defn edit
  "Replaces the node at this loc with the value of (f node args)"
  [^ZipperLocation loc f & args]
  (replace loc (apply f (.node loc) args)))

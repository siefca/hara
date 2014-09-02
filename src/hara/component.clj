(ns hara.component
  (:require [hara.data.nested :refer [merge-nested]]
            [hara.data.record :refer [empty-record]]
            [hara.sort.topological :refer [topological-sort]]))

(defprotocol IComponent
  (-start [component])
  (-stop  [component])
  (-started? [component])
  (-stopped? [component]))

(extend-protocol IComponent
  Object
  (-start [this] this)
  (-stop  [this] this))

(defn component? [x]
  (extends? IComponent (type x)))

(defn started?
  "checks if a component has been started

  (started? (Database.))
  => false

  (started? (start (Database.)))
  => true

  (started? (stop (start (Database.))))
  => false"
  {:added "2.1"}
  [component]
  (try (-started? component)
       (catch IllegalArgumentException e
         (-> component meta :started true?))
       (catch AbstractMethodError e
         (-> component meta :started true?))))

(defn stopped?
  "checks if a component has been stopped

  (stopped? (Database.))
  => true

  (stopped? (start (Database.)))
  => false

  (stopped? (stop (start (Database.))))
  => true"
  {:added "2.1"}
  [component]
  (try (-stopped? component)
       (catch IllegalArgumentException e
         (-> component started? not))
       (catch AbstractMethodError e
         (-> component started? not))))

(defn start
  "starts a component/array/system

  (start (Database.))
  => (just {:status \"started\"})"
  {:added "2.1"}
  [component]
  (let [cp (-start component)]
    (if (instance? clojure.lang.IObj cp)
      (vary-meta cp assoc :started true)
      cp)))

(defn stop
  "stops a component/array/system

  (stop (start (Database.)))
  => (just {})"
  {:added "2.1"}
  [component]
  (let [cp (-stop component)]
    (if (instance? clojure.lang.IObj cp)
      (vary-meta cp dissoc :started)
      cp)))

(declare system? array? start-array stop-array)

(deftype ComponentArray [arr]
  Object
  (toString [this]
    (str "#arr"
         (mapv (fn [v]
                 (cond (or (system? v)
                           (array? v)
                           (not (component? v)))
                      v

                      :else
                      (reduce (fn [m [k v]]
                                (cond (extends? IComponent (type v))
                                      (update-in m ['*] (fnil #(conj % k) []))

                                      :else
                                      (assoc m k v)))
                              (empty-record v)
                              v)))
               arr)))

  IComponent
  (-start [this] (start-array this))
  (-stop  [this] (stop-array  this))

  clojure.lang.Seqable
  (seq [this] (seq arr))

  clojure.lang.IObj
  (withMeta [this m]
    (ComponentArray. (with-meta arr m)))

  clojure.lang.IMeta
  (meta [this] (meta arr)))

(defmethod print-method ComponentArray
  [v w]
  (.write w (str v)))

(defn start-array [carr]
  (with-meta
    (ComponentArray. (mapv start (seq carr)))
    (meta carr)))

(defn stop-array [carr]
  (with-meta
    (ComponentArray. (mapv stop (seq carr)))
    (meta carr)))

(defn array
  "creates an array of components

  (let [recs (start (array map->Database [{:id 1} {:id 2}]))]
    (count (seq recs)) => 2
    (first recs) => (just {:id 1 :status \"started\"}))"
  {:added "2.1"}
  [ctor config]
  (if (vector? config)
    (let [all (meta config)]
      (ComponentArray. (mapv (fn [cfg]
                               (ctor (merge-nested all cfg))) config)))))

(defn array?
  "checks if object is a component array

  (array? (array map->Database []))
  => true"
  {:added "2.1"}
  [x]
  (instance? ComponentArray x))

(declare start-system stop-system)

(defrecord ComponentSystem []
  Object
  (toString [sys]
    (str "#sys"
         (reduce (fn [m [k v]]
                   (cond (or (system? v)
                             (array? v)
                             (not (component? v)))
                         (assoc m k v)

                         :else
                         (assoc m k (reduce (fn [m [k v]]
                                              (cond (extends? IComponent (type v))
                                                    (update-in m ['*] (fnil #(conj % k) []))

                                                    :else
                                                    (assoc m k v)))
                                            (empty-record v)
                                            v))))
                 {} sys)))

  IComponent
  (-start [sys]
    (start-system sys))
  (-stop [sys]
    (stop-system sys)))

(defmethod print-method ComponentSystem
  [v w]
  (.write w (str v)))

(defn- system-dependencies [topology]
  (reduce (fn [m [k v]]
            (->> (rest v)
                 (map (fn [dep]
                        (if (vector? dep)
                          (first dep)
                          dep)))
                 (set)
                 (assoc m k)))
   {}
   topology))

(defn- system-constructors [topology]
  (reduce (fn [m [k v]]
            (assoc m k (first v)))
   {}
   topology))

(defn- system-augmentations [topology]
  (reduce (fn [m [k v]]
            (->> (rest v)
                 (set)
                 (assoc m k)))
   {}
   topology))

(defn- augmentation-fn [csys aug]
  (fn [i cp]
    (reduce (fn [cp dk]
              (cond (vector? dk)
                    (let [[dk ek] dk
                          dcp  (get csys dk)]
                      (assoc cp ek (nth (seq dcp) i)))

                    :else
                    (assoc cp dk (get csys dk))))
            cp aug)))

(defn start-system [csys]
  (let [graph (meta csys)
        cmp-keys  (-> graph :dependencies topological-sort)
        cmp-ctors (-> graph :constructors)
        cmp-augs  (-> graph :augmentations)]
    (-> (reduce (fn [m k]
                  (let [component  (get csys k)
                        ctor       (get cmp-ctors k)
                        aug        (get cmp-augs k)]
                    (assoc m k (cond (vector? ctor)
                                     (->> (seq component)
                                          (map-indexed (augmentation-fn m aug))
                                          (ComponentArray.)
                                          (start))

                                     :else
                                     (-> component
                                         (merge (select-keys m aug))
                                         start)))))
                (ComponentSystem.)
                cmp-keys)
        (with-meta graph))))

(defn- de-augmentation-fn [csys aug]
  (fn [i cp]
    (reduce (fn [cp dk]
              (cond (vector? dk)
                    (let [[dk ek] dk]
                      (dissoc cp ek))

                    :else
                    (dissoc cp dk)))
            cp aug)))

(defn stop-system [csys]
  (let [graph (meta csys)
        cmp-keys  (-> graph :dependencies topological-sort)
        cmp-ctors (-> graph :constructors)
        cmp-augs  (-> graph :augmentations)]
    (reduce (fn [m k]
              (let [component  (get csys k)
                    ctor       (get cmp-ctors k)
                    aug        (get cmp-augs k)]
                (assoc m k (cond (vector? ctor)
                                 (->> (stop component)
                                      (map-indexed (de-augmentation-fn m aug))
                                      (vec)
                                      (ComponentArray.))

                               :else
                               (apply dissoc (stop component) aug)))))
            {}
            (reverse cmp-keys))))

(defn system
  "creates a system of components

  (let [topo {:db     [map->Database]
              :files  [[map->Filesystem]]
              :store  [[map->Database] [:files :fs] :db]}
        cfg  {:db {:type :basic :host \"localhost\" :port 8080}
              :files [{:path \"/app/local/1\"} {:path \"/app/local/2\"}]
              :store [{:id 1} {:id 2}]}
        sys (-> (system topo cfg) start)]

    (:db sys) => (just {:status \"started\", :type :basic, :port 8080, :host \"localhost\"})

    (-> sys :files seq first) => (just {:status \"started\", :path \"/app/local/1\"})

    (-> sys :store seq first keys))  => (just [:status :fs :db :id] :in-any-order)"
  {:added "2.1"}
  [topology config]
  (let [deps  (system-dependencies  topology)
        ctors (system-constructors  topology)
        augms (system-augmentations topology)]
    (with-meta
      (reduce (fn [sys [k ctor]]
                (assoc sys k (cond (nil? ctor) config

                                   (vector? ctor)
                                   (let [arr-cfg (get config k)]
                                     (if (vector? arr-cfg)
                                       (array (first ctor) arr-cfg)
                                       (throw (Exception. (str "config for component " k
                                                               " has to be a vector.")))))

                                   :else
                                   (ctor (get config k)))))
              (ComponentSystem.)
              ctors)
      {:dependencies deps
       :constructors ctors
       :augmentations augms})))

(defn system?
  "checks if object is a component system

  (system? (system {} {}))
  => true"
  {:added "2.1"}
  [x]
  (instance? ComponentSystem x))

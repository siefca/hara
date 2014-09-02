(ns hara.component-test
  (:use midje.sweet)
  (:require [hara.component :refer :all]))

(defrecord Database []
    IComponent
    (-start [db]
      (assoc db :status "started"))
    (-stop [db]
      (dissoc db :status)))

(defrecord Filesystem []
    IComponent
    (-start [db]
      (assoc db :status "started"))
    (-stop [db]
      (dissoc db :status)))

^{:refer hara.component/started? :added "2.1"}
(fact "checks if a component has been started"

  (started? (Database.))
  => false

  (started? (start (Database.)))
  => true

  (started? (stop (start (Database.))))
  => false)

^{:refer hara.component/stopped? :added "2.1"}
(fact "checks if a component has been stopped"

  (stopped? (Database.))
  => true

  (stopped? (start (Database.)))
  => false

  (stopped? (stop (start (Database.))))
  => true)


^{:refer hara.component/start :added "2.1"}
(fact "starts a component/array/system"

  (start (Database.))
  => (just {:status "started"}))

^{:refer hara.component/stop :added "2.1"}
(fact "stops a component/array/system"

  (stop (start (Database.)))
  => (just {}))

^{:refer hara.component/array :added "2.1"}
(fact "creates an array of components"

  (let [recs (start (array map->Database [{:id 1} {:id 2}]))]
    (count (seq recs)) => 2
    (first recs) => (just {:id 1 :status "started"})))

^{:refer hara.component/array? :added "2.1"}
(fact "checks if object is a component array"

  (array? (array map->Database []))
  => true)


^{:refer hara.component/system :added "2.1"}
(fact "creates a system of components"

  (let [topo {:db     [map->Database]
              :files  [[map->Filesystem]]
              :store  [[map->Database] [:files :fs] :db]}
        cfg  {:db {:type :basic :host "localhost" :port 8080}
              :files [{:path "/app/local/1"} {:path "/app/local/2"}]
              :store [{:id 1} {:id 2}]}
        sys (-> (system topo cfg) start)]

    (:db sys) => (just {:status "started", :type :basic, :port 8080, :host "localhost"})

    (-> sys :files seq first) => (just {:status "started", :path "/app/local/1"})

    (-> sys :store seq first keys))  => (just [:status :fs :db :id] :in-any-order))


^{:refer hara.component/system? :added "2.1"}
(fact "checks if object is a component system"

  (system? (system {} {}))
  => true)

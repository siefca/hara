(ns hara.protocol.watch)

(defprotocol IObservable
  (-add-watch  [obj opts observer])
  (-list-watch [obj opts])
  (-remove-watch [obj opts]))
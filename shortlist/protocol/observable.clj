(ns hara.protocol.observable)

(defprotocol IObservable
  (-add-observer [obj opts observer])
  (-list-observers [obj opts])
  (-remove-observer [obj opts]))
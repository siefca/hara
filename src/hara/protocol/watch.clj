(ns hara.protocol.watch)

(defprotocol IWatch
  (-add    [obj opts observer])
  (-list   [obj opts])
  (-remove [obj opts]))
(ns hara.protocol.watch
  (:require [hara.expression.shorthand :refer [get->]]))

(defprotocol IWatch
  (-add-watch    [obj opts f])
  (-list-watch   [obj opts])
  (-remove-watch [obj opts]))
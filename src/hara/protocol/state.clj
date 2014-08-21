(ns hara.protocol.state)

(defprotocol IStateGet
  (-get-state [obj opts]))

(defprotocol IStateSet
  (-update-state [obj opts f args])
  (-set-state [obj opts v])
  (-empty-state [obj opts]))
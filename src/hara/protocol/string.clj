(ns hara.protocol.string)

(defprotocol IString
  (-to-string [x])
  (-to-string-meta [x]))

(ns metis.protocols)

(defprotocol Includable
  (includes? [this item]))
 
(defprotocol AsString
  (->string [this]))
 


(ns metis.util
  (:require [metis.protocols :as protocols]
            [clojure.string :as cstr]))

(defn blank? [attr]
  (cond
    (string? attr) (cstr/blank? attr)
    (coll? attr) (empty? attr)
    :else false))

(defn present? [attr]
  (not (or (blank? attr) (nil? attr))))

;; TODO cond here
(defn formatted? [attr pattern]
  (when (nil? pattern)
    (throw (Exception. "Pattern to match with not given.")))
  (when (not (nil? attr))
    (not (nil? (re-matches pattern attr)))))

;; TODO parseInt here
(defn str->int [s]
  (try
    (Integer. s)
    (catch NumberFormatException e)))

;; TODO parseFloat?  Probably 
(defn str->float [s]
  (try
    (Float. s)
    (catch NumberFormatException e)))

(defn keyword->str [k]
  (str (name k)))

(extend-protocol protocols/Includable
  clojure.lang.Seqable
  (includes? [this item]
    (not (nil? (some #(= item %) this)))))

(def capital #"[A-Z]")
(defn spear-case [s]
  (let [s (or (cstr/replace-first s capital (fn [c] (cstr/lower-case c))) s)]
    (or (cstr/replace s capital (fn [c] (str "-" (cstr/lower-case c)))) s)))


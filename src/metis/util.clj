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

(defn formatted? [attr pattern]
  (cond
    (nil? pattern) (throw (Exception. "Pattern to match with not given."))
    (not (nil? attr)) (not (nil? (re-matches pattern attr)))))

(defn str->int [s]
  (try
    (Integer/parseInt s)
    (catch NumberFormatException e)))

(defn str->float [s]
  (try
    (Float/parseFloat s)
    (catch NumberFormatException e)))

(extend-protocol protocols/Includable
  clojure.lang.Seqable
  (includes? [this item]
    (not (nil? (some #(= item %) this)))))

(def capital #"[A-Z]")
(defn spear-case [s]
  (let [s (or (cstr/replace-first s capital (fn [c] (cstr/lower-case c))) s)]
    (or (cstr/replace s capital (fn [c] (str "-" (cstr/lower-case c)))) s)))


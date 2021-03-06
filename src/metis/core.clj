(ns metis.core
  (:require [metis.protocols :as protocols])
  (:use [metis.util]
        [clojure.set :only [union]]))

(defn should-run? [record attr options context]
  (let [{:keys [allow-nil allow-blank allow-absence only except if]
         :or {allow-nil false
              allow-blank false
              allow-absence false
              only []
              except []
              if (fn [attrs] true)}} options
        allow-nil (if allow-absence true allow-nil)
        allow-blank (if allow-absence true allow-blank)
        only (flatten [only])
        except (flatten [except])
        value (attr record)
        if-condition (or (:if options) (fn [attrs] true))
        if-not-condition (or (:if-not options) (fn [attrs] false))]
    (not (or
      (and allow-nil (nil? value))
      (and allow-blank (blank? value))
      (and context (seq only) (not (includes? only context)))
      (and context (seq except) (includes? except context))
      (not (if-condition record))
      (if-not-condition record)))))

;; TODO Refactor this to pull apart name/symbol dance, and accept Fns
(extend-protocol protocols/AsString
  clojure.lang.Keyword
  (->string [this] (name this))

  java.lang.String
  (->string [this] this)

  clojure.lang.Symbol
  (->string [this] (name this)))

(defn- validator-name [name]
  (symbol (->string name)))

(defn- validator-factory [name]
  (let [name (validator-name name)]
    (or
      (resolve name)
      (throw (Exception. (str "Cound not find validator " name ". Looked in " *ns* " for " name "."))))))

(defn- run-validation [map key validator options context]
  (when (should-run? map key options context)
    (validator map key options)))

(defn- run-validations [map key validations context]
  (reduce
    (fn [errors [validator options]]
      (if-let [error (run-validation map key validator options context)]
        (if (seq error)
          (conj errors (or (:message options) error))
          errors)
        errors))
    []
    validations))

(defn- normalize-errors [errors]
  (if (and (= 1 (count errors)) (map? (first errors)))
    (first errors)
    errors))

(defn -validate [record validations context]
  (reduce
    (fn [errors [attr attr-vals]]
      (let [attr-errors (run-validations record attr attr-vals context)]
        (if (every? empty? attr-errors)
          errors
          (assoc errors attr (normalize-errors attr-errors)))))
    {}
    validations))

(defn -parse-validations [validations]
  (loop [validations validations ret []]
    (if (empty? validations)
      ret
      (let [[cur next] validations]
        (cond
          (map? next)
            (recur (rest (rest validations)) (conj ret [(validator-factory cur) next]))
          (keyword? next)
            (recur (rest validations) (conj ret [(validator-factory cur) {}]))
          (nil? next)
            (recur [] (conj ret [(validator-factory cur) {}])))))))

(defn -parse
  ([attrs validations & args]
   [(flatten attrs) (-parse-validations (flatten [validation args]))]))

(defn -merge-validations [validations]
  (apply merge-with union {} validations))

(defn -expand-validation [validation]
  (let [[attributes validations] (apply -parse validation)]
    (-merge-validations
      (for [attr attributes validation validations]
        {attr #{validation}}))))

(defn -expand-validations [validations]
  (-merge-validations (map -expand-validation validations)))

;(defmacro defvalidator [name & validations]
;  (def name (make-a-validator-fn validations)))

(defmacro defvalidator [name & validations]
  (let [name (validator-name name)
        validations (vec validations)]
    `(let [validations# (-expand-validations ~validations)]
       (defn ~name
         ([record#] (-validate record# validations# nil))
         ([record# context#] (-validate record# validations# context#))
         ([record# attr# options#] (~name (attr# record#)))))))


(ns draconic.reversing-fn
  (:require [clojure.string :as str]))

(defn create-reversing-fn
  "Returns a vector of two functions and an atom- [a fn of x to y, a fn of y to x if the first function has already been called on x, and the atom that holds the cache in the form of a map of y to x]."
  [the-fn]
  (let [stash-atom (atom {})
        stashing-fn (fn [the-arg]
                      (let [the-result (the-fn the-arg)]
                        (swap! stash-atom #(into % {the-result the-arg}))
                        the-result
                        ))
        unstash-fn (fn [the-arg]
                     (get @stash-atom the-arg))]
    [stashing-fn unstash-fn stash-atom]))

(defn original
  "Gets the function of x to y from a result of create-reversing-fn. Useful when the result is stored in a var."
  [creation-result]
  (first creation-result))

(defn reversing
  "Gets the function of y to x from a result of create-reversing-fn. Useful when the result is stored in a var."
  [creation-result]
  (second creation-result))

(comment
  (let [replace-o #(-> %
                       (str/replace "-" " ")
                       (str/replace ":" ""))
        the-fn (fn [a] (-> a str replace-o str/capitalize))
        [reg-fun unstash-fn the-atom] (create-reversing-fn the-fn)
        passthrough (fn [thing] (clojure.pprint/pprint thing) thing)
        result-o (-> :whats-a-matter-you?
                     passthrough
                     reg-fun
                     passthrough
                     unstash-fn
                     passthrough)]

    result-o
    )
  )

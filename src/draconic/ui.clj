(ns draconic.ui
  {:doc "This namespace is primarily intended to make writing UI logic idomatic and generic for Clojure. Contrasting with frameworks like Reagent and Om, there is no underlying implimentation assumed by this ns at the abstraction level. Node initialization, lifecycle management, and markup is best done using the facilities of the frameworks themselves.

  The general design goal is for an application developer to be able to write UI bindings for the business domain in a generic and portable way, that can then be applied to each platform's spcific UIs using a unified and sensical interface.

  An additional design goal is to make the API for dealing with UI frameworks simpler and more consistant (both within frameworks and between them). UI elements are all functionally similar; a text box or button doesn't behave differently (from a user's perspective) between platforms, so why can't we write code that treats them as the same things?"}
  (:require [draconic.ui.atomic-node :as b]))

(defn get-id [node]
  (b/-ui-get-id node))
(defn set-id! [node new-id]
  (b/-ui-set-id! node new-id))

(defn deref [node]
  (b/-ui-get-state node))

(defn reset!
  "Takes a new val, that is set as the node's new state without consideration for the current state."
  [node newval]
  (b/-ui-set-state! node newval))

(defn compare-and-set!
  "Sets the new value if, and only if, the state of the node at call time is the same as the state of the node at set time. Returns true if set successful, else false."
  [node newval]
  (let [orig-state (deref node)]
    (if (= orig-state (deref node))
      (do
        (reset! node newval)
        true)
      false)))

(defn swap!
  "Takes a function of (old state) -> (new state). Works like with Clojure's swap!, so the fn should be without side effects. Returns the node."
  [node swap-fn]
  (loop []
    (if (compare-and-set! node (swap-fn (deref node)))
      node
      (recur)))
  )

(defn deref-all
  "Gets the current state of all nodes in a seq, returning a lazy seq of maps of string-id to derefed-state."
  [seq-of-nodes]
  (map (fn [nodio] {(get-id nodio) (deref nodio)})))

(defn get-event-chan
  "Gets the core.async channel currently set as the event channel for the given node. Returns nil if no chan is present."
  [node]
  (b/-ui-get-event-chan node))

(defn set-event-chan!
  "Sets a channel onto which events are broadcast. Returns the node. If a chan is already set, this is a no-op."
  [node event-chan]
  (when (get-event-chan node)
    (b/-ui-set-event-chan! node event-chan))
  node)

(defn remove-event-chan!
  "Removes the currently-set event channel so that a new one can be set. Returns the node."
  [node]
  (b/-ui-remove-event-chan node)
  node
  )


(defn get-ext [node]
  (b/-ui-get-ext node))

(defn reset-ext! [node newopts]
  (b/-ui-set-ext! node newopts))

(defn add-ext! [node newopt]
  (let [oldopts (get-ext node)]
    (reset-ext! node (conj oldopts newopt))))

(defn remove-ext! [node badopt]
  (let [oldopts (get-ext node)
        newopts (filter #(not (= % badopt)) oldopts)]
    (reset-ext! node newopts)))



(defn get-state-spec [node]
  (b/-ui-get-state-spec node))

(defn set-state-spec [node newspec]
  (b/-ui-set-state-spec node newspec))

(ns draconic.ui
  {:doc "This namespace contains functions for dealing with Atomic Nodes. It is strongly recommended that client code use this functions *instead* of the protocol functions directly, as these functions can be instrumented while protocol functions canot. Additionally, these functions exist to remove some boilerplate for commonly executed tasks, such as getting/setting *only* the node's state, adding/removing options from :option class nodes, etc."}
  (:require [draconic.ui.atomic-node :as an]
            [clojure.core.async :as as]))

(defn get-node-data [node]
  (an/-ui-get-node-data node))
(defn request-set-node-data [node new-data]
  (an/-ui-request-set-node-data node new-data))

(defn get-attribute [node attribute-keyword]
  (get (get-node-data node) attribute-keyword))
(defn set-attribute! [node attribute-keyword new-value]
  (request-set-node-data node {attribute-keyword new-value}))

(defn get-id [node]
  (get-attribute node :id))
(defn set-id! [node new-id]
  (set-attribute! node :id new-id))

(defn deref [node]
  (get-attribute node :state))

(defn reset!
  "Takes a new val, that is set as the node's new state without consideration for the current state."
  [node newval]
  (set-attribute! node :state newval))

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


(def new-chan-fn
  "Atom containing a function returning a core.async chan. Used by functions in this ns to make a new chan."
  (atom (fn [] (as/chan))))
(defn get-event-chan
  "Gets the core.async channel currently set as the event channel for the given node."
  [node]
  (get-attribute node :event-pub))
(defn get-event-pub
  "Returns a pub of the event-chan dispatching on :event-category. Either gets the existant pub or makes one if none exist."
  [node]
  (let [got-pub (get-attribute node :event-pub)
        the-pub (if got-pub got-pub (as/pub (get-event-chan node) :event-category))]
    (when (not got-pub) (set-attribute! node :event-pub the-pub))
    the-pub
    ))
(defn add-event-callback
  "Adds a callback that will recieve all events of a given category from the node. The callback should be a fn that takes the event map, which includes the parent node under :parent; anything that the fn returns will be output on the returned chan. Callbacks are not deletable once set, only closing the event chan (or calling reset-chan! which does that and more) will stop callback functions from recieving further events. Returns a core.async chan."
  [node event-type callback-fn]
  (let [the-pub (get-event-pub node)
        dedicated-event-chan (@new-chan-fn)
        dedicated-return-chan (as/chan (as/sliding-buffer 5))]
    (as/go-loop []
      (let [the-event (as/<! dedicated-event-chan)]
        (as/>! dedicated-return-chan (callback-fn the-event)))
      (recur))
    (as/sub the-pub event-type dedicated-event-chan)
    dedicated-return-chan))
(defn reset-chan!
  "Resets the chan, any callbacks, and any pubs."
  ([node]
   (reset-chan! node (@new-chan-fn)))
  ([node new-chan]
   (as/close! (get-event-chan node))
   (request-set-node-data node {:event-chan new-chan :event-pub nil})
   node)
  )


(defn get-state-spec [node]
  (get-attribute node :state-spec))

(defn set-state-spec! [node newspec]
  (set-attribute! node :state-spec newspec))

(defn get-options-list [node]
  (get-attribute node :options))
(defn set-options-list! [node new-options]
  (set-attribute! node :options new-options))
(defn add-option! [node new-option]
  (let [existant-options (get-options-list node)
        new-options (if existant-options
                      (conj existant-options new-option)
                      [new-option])]
    (set-options-list! node new-options)
    ))
(defn remove-option! [node bad-option]
  (let [existant-options (get-options-list node)
        new-options (if existant-options
                      (filter #(not (= % bad-option)) existant-options)
                      [])]
    (set-options-list! node new-options)
    ))

(defn default-render-fn [thing]
  (if (instance? clojure.lang.Named thing)
    (name thing)
    (str thing)))
(defn get-render-fn [node]
  (get-attribute node :render-fn))
(defn set-render-fn! [node new-fn]
  (set-attribute! node :render-fn new-fn))
(defn render-fn-to-default! [node]
  (set-attribute! node :render-fn default-render-fn))
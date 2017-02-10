(ns draconic.ui
  {:doc "This namespace contains functions for dealing with Atomic Nodes. It is strongly recommended that client code use this set of functions *instead* of the protocol functions directly, as these functions can be instrumented while protocol functions canot, extra logic can be put in place, etc. Additionally, these functions exist to remove some boilerplate for commonly executed tasks, such as getting/setting *only* the node's state, adding/removing options from :option class nodes, etc."}
  (:require [draconic.ui.atomic-node :as an]
            [clojure.core.async :as as]
            [clojure.core :as core]
            [clojure.string :as str]
            [clojure.data :as data]
            [draconic.macros :as dm]
            [draconic.reversing-fn :as rev])
  (:import (clojure.lang Atom)))

(declare apply-render-fn!)
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
  (let [unstasher (or (get-attribute node :unstash-fn) (fn [i] i))] (unstasher (get-attribute node :state))))

(defn reset!
  "Takes a new val, that is set as the node's new state without consideration for the current state."
  [node newval]
  (let [renderer (or (get-attribute node :render-fn) (fn [i] i))]
    (set-attribute! node :state (renderer newval))))

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
      (recur))))


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
  (let [orig-chan (get-attribute node :event-chan)
        proper-chan (if orig-chan orig-chan (@new-chan-fn))]
    (set-attribute! node :event-chan proper-chan)
    proper-chan))
(defn get-event-pub
  "Returns a pub of the event-chan dispatching on :event-category. Either gets the existant pub or makes one if none exist."
  [node]
  (let [got-pub (get-attribute node :event-pub)
        the-pub (if got-pub got-pub (as/pub (get-event-chan node) :event-category))]
    (when (not got-pub) (set-attribute! node :event-pub the-pub))
    the-pub))

(defn add-event-callback
  "Adds a callback that will recieve all events of a given category from the node. The callback should be a fn that takes the event map, which includes the parent node under :parent; anything that the fn returns will be output on the returned chan. Callbacks are not deletable once set, only closing the event chan (or calling reset-chan! which does that and more) will stop callback functions from recieving further events. Returns a core.async chan."
  [node event-type callback-fn]
  (let [boss-chan (get-event-chan node)
        the-pub (get-event-pub node)
        dedicated-event-chan (@new-chan-fn)
        dedicated-return-chan (as/chan (as/sliding-buffer 5))]
    (as/sub the-pub event-type dedicated-event-chan)
    (as/go-loop []
      (let [the-event (as/<! dedicated-event-chan)]
        (if the-event
          (do
            (println "event chan is " dedicated-event-chan)
            (try
              (as/>! dedicated-return-chan (callback-fn the-event))
              (catch Exception e
                (as/>! dedicated-return-chan ["callback threw error: " e])))
            (recur))
          nil)))
    dedicated-return-chan))


(extend-protocol clojure.core.async.impl.protocols/Channel
  nil
  (close! [it] "tried to close a nil?")
  (closed? [it] true))
(defn reset-chan!
  "Resets the chan, any callbacks, and any pubs."
  ([node]
   (reset-chan! node (@new-chan-fn)))
  ([node new-chan]
   (as/close! (get-event-chan node))
   (request-set-node-data node {:event-chan new-chan :event-pub nil})
   node))



(defn get-state-spec [node]
  (get-attribute node :state-spec))

(defn set-state-spec! [node newspec]
  (set-attribute! node :state-spec newspec))

(defn get-options-list [node]
  (get-attribute node :options))
(defn set-options-list! [node new-options]
  (set-attribute! node :options new-options)
  (apply-render-fn! node))
(defn add-option! [node new-option]
  (let [existant-options (get-options-list node)
        new-options (if existant-options
                      (conj existant-options new-option)
                      [new-option])]
    (set-options-list! node new-options)))

(defn remove-option! [node bad-option]
  (let [existant-options (get-options-list node)
        new-options (if existant-options
                      (filter #(not (= % bad-option)) existant-options)
                      [])]
    (set-options-list! node new-options)))


(defn- tostr [t]
  (if (instance? clojure.lang.Named t)
    (name t)
    (str t)))
(def ^:private default-unstash-atom (atom {}))
(def ^:private default-render-fn-set
  (rev/create-reversing-fn #(let [resulto (-> %
                                              tostr
                                              (str/replace "-" " ")
                                              str/capitalize)]
                              resulto)))
(def default-render-fn ^{:arglists '([thing])
                        :doc "Converts to string (via name if possible), replaces '- 'with space, and capitalizes it."}
  (rev/original default-render-fn-set))

(def default-reversal-fn ^{:arglists '([thing])
                          :doc "Returns the arg that produced the thing when given to default-render-fn"}
  (rev/reversing default-render-fn-set))

(defn get-render-fn
  "Gets the render-fn for the node. If none is present, returns draconic.ui/default-render-fn."
  [node]
  (let [fn-in-map (get-attribute node :render-fn)]
    fn-in-map))
(defn apply-render-fn!
  "Maps a fn of item to string across the node's provided options, adding it to the node under :rendered-options (which platforms should use to populate list views rather then raw :options). Also adds :unstash-fn, which reverses the call and gets the original value back. :render-fn and :unstash-fn can be used together until the next call to apply-render-fn! with a new fn, after which new ones are generated. It is up to the implimenting platform how often apply-render-fn! is called within the implimentation, but it should always be true that modifications to :options are reflected in :rendered-options and apply-render-fn! makes that simple.

  If the node has a :render-fn already, calling with one argument re-uses the same render-fn and unstash-fn, only modifying the :rendered-options (if there is no :render-fn, calling with one argument adds the default render function to the node). Calling it with a new fn replaces all three in the node's map.

  Only reliable if :options contains immutable elements."
  ([node] (apply-render-fn! node (get-render-fn node) false))
  ([node new-fn] (apply-render-fn! node new-fn true))
  ([node new-fn set-new?]
   (let [[orig-fn rev-fn] (cond
                            (and set-new? new-fn) (rev/create-reversing-fn new-fn)
                            (not new-fn) default-render-fn-set
                            :default [new-fn (get-attribute node :unstash-fn)])
         orig-options (get-options-list node)]
     (set-attribute! node :rendered-options (map #(orig-fn %) orig-options))
     (when (or set-new? (not new-fn))
       (set-attribute! node :unstash-fn rev-fn)
       (set-attribute! node :render-fn orig-fn)))))

(defn render-fn-to-default!
  "Resets the node's render function to the default."
  [node]
  (apply-render-fn! node nil))


(defn make-state-change-event
  "Convenience function for making a state-change event map. Takes "
  [parent-node new-state old-state & extra]
  (into
    {:event-category :state-change
    :parent         parent-node
    :new-state      new-state
    :old-state      old-state}
    (apply hash-map extra)))

(extend-protocol an/Atomic-Node

  Atom
  (-ui-get-node-data [this]
    (let [nstate @this]
      (if (map? nstate)
        nstate
        {:class [:none]
         :state "Atom " this " is not an Atomic Node."})))
  (-ui-request-set-node-data [this new-node-data]
    (let [nstate (get-node-data this)
          [_ diffstate _] (data/diff nstate new-node-data)]

      (when (:event-chan diffstate)
        (add-watch this :state-change
                   (fn [the-key the-atom oldstate newstate]
                     (let [[_ t-ds _] (data/diff oldstate newstate)]
                       (println "thing being put in chan-> " newstate)
                       (if (:state t-ds)
                         (as/go (as/>! (:event-chan diffstate)
                                       (make-state-change-event
                                         this (:state newstate) (:state oldstate))
                                       )))))))


      (core/swap! this (fn [c-s] (into c-s new-node-data))))))



(comment
  (def test-node (atom {:class   [:text :options]
                        :state   ""
                        :options []}))

  (set-options-list! test-node [:something :nothing :everything :a-whisper])

  (apply-render-fn! test-node default-render-fn)

  (add-option! test-node :billions)

  (clojure.pprint/pprint (core/deref test-node))
  )

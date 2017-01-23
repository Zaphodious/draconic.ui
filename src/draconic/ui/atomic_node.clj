(ns draconic.ui.atomic-node)

(defprotocol Atomic-Node
  "Set of functions that should be supported by all nodes. Functions from the draconic.ui namespace should be used instead of these in all production code (as those functions can be specc'd, instrumented, etc, and are thus less open to erronious use). Each funciton here is prefixed with '-ui-' to help make make this easy to remember."
  (-ui-get-attributes [this] "An immutable map of attributes that describes the extending node. Used by the generic binding code to ensure that information is going into places that can accept it.

  Guaranteed to be present (each key mapping to a single keyword):
  :state-type #{:text :boolean :number :node}
  :state-coll-type #{:single :seq :multi-seq}
  :node-type #{:user-editable :display :layout}
  ")
  (-ui-get-id [this] "Gets a string identifier for this node. Only nodes that return a valid (non-nill, non-empty string) result should be operated on by non-instantiation code, all others being considered implimentation details.")
  (-ui-set-id! [this new-id] "Sets the new ID string on the node.")
  (-ui-get-state [this] "Gets the main data from this node, which will be different depending on the node in question. A TextField, for instance, gets a String, while a checkbox gets a boolean. Only used for nodes with significant information. A node with children will use Container-Node.")
  (-ui-set-state! [this newval] "The second arg is set as the node's new state. Should be in the same form as -ui-get-state's return.")
  (-ui-get-event-chan [this] "Gets the core.asyn chan set previously, that events are put into. Returns nil unless a chan is actually set.")
  (-ui-set-event-chan! [this event-chan] "Sets a core.async chan into which event notifications are placed. Changes are transmitted in a map {:event-type <:type-of-event> :event-details <event details>}. Change events, for example, look like {:event-type :change-event :event-details {:old-state <old value> :new-state <new value>}")
  (-ui-remove-event-chan [this] "Removes the event-chan from the node, allowing a new one to be set. Future cals to -ui-get-event-chan should return nil.")
  (-ui-get-state-spec [this] "Gets the validator spec set on the node. The spec will be a namespace-qualified keyword (unnamed specs aren't supported), and all state going into or out of the node will be conformed to this spec. Default spec is core/any?")
  (-ui-set-state-spec [this newspec] "Sets the validator spec set on the node. Newspec must be a namespace qualified keyword. All state going into or out of the node will be conformed to the provided spec. Default spec is core/any")
  (-ui-get-ext [this] "Gets the extention map for this node. Not guaranteed to be the same map passed into previous calls to -ui-set-ext!.")
  (-ui-set-ext! [this new-exts] "Sets the extention map. Used by each node in different ways. Standard extentions are- :choices, taking a set of exclusive options, ")
  )
(ns draconic.ui.atomic-node)

(defprotocol Atomic-Node
  "Set of functions that should be supported by all nodes. Functions from the draconic.ui namespace should be used instead of these in all production code (as those functions can be specc'd, instrumented, etc, and are thus less open to erronious use). Each funciton here is prefixed with '-ui-' to help make make this easy to remember."

  (-ui-get-node-data [this] "Gets the data map for this node. Different node classes (that is, classes in the grouping sense, not the OOP sense) will have different requirements for this map, but all node classes will provide the following keys:

:class - The class of the node itself.
:id - A string ID for this node. Values of empty-string or nil indicate that the node is an implimentation detail for the target, and thus should not be operated on by any but non-initialization code.
:state - A representation of the class' internal state. A Text Box will give a string, a Check Box will give a boolean, a container pane will give a list or map of lists of nodes, etc.
:user-editable? - If the node supports the user setting an arbitrary state. A drop-down menu, a label, and a spinner would all be false, and a text box would be true. Important for :option class nodes, a user-editable of true means that arbitrary answers can be given that don't necessary match the :options. As such, if a set of exclusive results is required, it is a good idea to test for this property and throw an exception if true.
:state-spec - The spec used to validate the :state. Defaults to any?, requires a namespace-qualified keyword (anon specs are not supported).
:event-chan - A core.async chan onto which events are placed.

The :option class has the following additional keys:

:options - A seq of options to be presented to the user.
:render-fn - A one-arg function that takes a thing from :options and returns a string. Defaults to draconic.ui/default-render-fn.

Please see classes.md in the documentation for this library, for further information about ui node classes (including what each class supports).

Also note that this should not be expected to be derived from any previous map passed into -ui-request-set-node-data. Some frameworks might not facilitate storing arbitrary data, and it is additionally important that this information can be scraped from nodes that have never had a set request performed.
"
    )
  (-ui-request-set-node-data [this new-node-data] "Sets the data for this node. Only keys present in this map will be effected, with non-indicated attributes retaining their previous states. Keys not relivant to the node will be ignored. Keys not changable (:class, sometimes :type) will be ignored. The map might be discarded after application, depending on the framework. Returns true if anything was changed, else returns false."))
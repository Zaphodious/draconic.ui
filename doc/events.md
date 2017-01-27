# Events

UIs, to a one, have faculties for dealing with dealing with player input. Each platform has a slightly different way of doing it, but enough is similar between them that we can abstract over them and thus program generically for them.

An event in dracoinc.ui is a notification that something significant has happened to a node. As draconic.ui is a library that doesn't assume any specific framework or platform, it also doesn't assume any particular representation of events. Like with Atomic Nodes, we work with the simplest possible data that conveys meaningful information, translating from the platform's idioms if necessary. Do note that _most_ event programming **should** be done on the client side. The event model is here to simplify common tasks that can be safely written for all intended platforms.

## Representation
Events are maps sent over core.async channels (or provided to callback functions).

Each event has an :event-category and a :parent. On certain platforms, the original platform event object might be included under its own keyword. Each category will have a different set of data included with the event, each with its own key.

### Categories
Events in draconic.ui are generally either an :action or a :state-change. Platforms may have additional action types, but all will have at least these two.

A state-change event is sent whenever the node's :state changes. The previous value is under :old-value, and the current value is under :new-value.

An action event is sent whenever the user interacts with the node in a way other then to change its state (this includes expanding a collapsible node). Information given (if available) is included under :keys-pressed, :mouse-buttons-clicked, and :selections (if the node class includes :selection, for convenience). If the event has a target node, that node will be included under :target. Keyboard-driven events will only be sent if the node is selected and the keyboard input doesn't change the state such that a state-change event is fired, or by the root node in the scene graph.
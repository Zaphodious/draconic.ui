(ns draconic.ui.mock.node
  (:require [draconic.ui.atomic-node :as an :refer [Atomic-Node]]
            [draconic.ui :as ui]
            [clojure.core.async :as as
             :refer [chan go go-loop >! <! >!! <!! pub sub]]))

(defn make-text-node [id]
  (let [the-atom (atom {:id             id
                        :class          [:text]
                        :state          ""
                        :user-editable? true
                        :state-spec     nil
                        :event-chan     (@ui/new-chan-fn)
                        })]
    (reify Atomic-Node
      (-ui-get-node-data [this]
        @the-atom)
      (-ui-request-set-node-data [this {:keys [state id class state-spec event-chan] :as new-node-data}]
        (let [now-state @the-atom]
          (when (and (not (nil? state)) (not (= state (:state now-state))))
            (go (>! (:event-chan now-state) {:event-category :state-change :parent this :old-value (:state now-state) :new-value state})))
          (reset! the-atom (into now-state new-node-data))))
      )))


#_(do
    (def sample-node
      (let [sample-text-node (make-text-node "a text node")]
       (ui/reset! sample-text-node "new value!")
       (ui/deref sample-text-node)
       (ui/get-event-pub sample-text-node)
       (ui/get-event-chan sample-text-node)
       (ui/add-event-callback sample-text-node :state-change #(do (println "the event is: " %) %)))
      )
    )


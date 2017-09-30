(ns animation-test
  (:require [animation.animation :refer :all]
            [clojure.test :as t]))

(t/deftest scratch
  (let [state-hook-did-run (atom false)
        state-hook (fn [atom-ref new-state]
                     (when (all-finished? new-state)
                       (reset! state-hook-did-run true)))
        state (state state-hook)
        screen (atom "")
        ani0 (fn [selection end]
               (swap! screen str "ani0")
               (end))
        ani1 (fn [selection end]
               (swap! screen str "ani1")
               (end))
        ani2 (fn [selection-map end]
               (swap! screen str "ani2")
               (end))]

    (add state ["test fader" :enter "chrome"] :selection ani0)
    (add state ["test animation" :enter "chrome"] :selection ani1)
    (add state ["test animation" :exit "chrome"] :selection ani1)
    (override state [["test animation" :enter "chrome"]
                     ["test animation" :exit  "chrome"]]
              ani2)

    (resolve state)
    (t/is ((comp not all-finished?) @state))
    (play state)
    (t/is (= "ani0ani2" @screen))
    (t/is @state-hook-did-run)))

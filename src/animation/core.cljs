(ns animation.core
  (:require
   [cljsjs.d3]
   [cljsjs.semantic-ui]
   [animation.render :as r]))

(enable-console-print!)

(def state (atom {:left
                  ["chrome", "firefox", "edge"]
                  :right
                  ["opera", "safari"]}))

(declare main)

(defn fade-in [selection]
  (.. selection
      (style "opacity" 0)
      transition
      (duration 2000)
      (style "opacity" 1))6)

(defn fade-out [selection]
  (.. selection
      transition
      (duration 2000)
      (style "opacity" 0)
      remove))

(defn swap-left-right [state-ref item]
  (swap! state-ref update :right #(conj % item))
  (swap! state-ref update :left #(remove #{item} %))
  (main))

(defn swap-right-left [state-ref item]
  (swap! state-ref update :left #(conj % item))
  (swap! state-ref update :right #(remove #{item} %))
  (main))

(defn root [state-ref]
  (let [state @state-ref]
    [:div.ui.two.column.grid.containeR
     {:join #(.. % (style "padding-top" "2em"))}

     [:div.column>:div.ui.list
      (for [item (:left state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true"))
                       :enter fade-in
                       :exit fade-out
                       :click #(swap-left-right state-ref  item)}])]
     [:div.column>:div.ui.list
      (for [item (:right state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true"))
                       :enter fade-in
                       :exit fade-out
                       :click #(swap-right-left state-ref item)}])]]))

(defn main []
  (println "***")
  (r/render
   (.. js/d3 (select "#app"))
   (root state)))

(main)

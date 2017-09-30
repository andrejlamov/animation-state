(ns animation.core
  (:require
   [cljsjs.d3]
   [cljsjs.semantic-ui]
   [animation.render :as r]))

(enable-console-print!)

(def data-state (atom {:left
                  #{ "chrome", "firefox", "edge" }
                  :right
                  #{ "opera", "safari" }}))

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

(defn swap-left-right [data-state-ref item]
  (swap! data-state-ref update :right #(conj % item))
  (swap! data-state-ref update :left #(remove #{item} %))
  (main))

(defn swap-right-left [data-state-ref item]
  (swap! data-state-ref update :left #(set (conj % item)))
  (swap! data-state-ref update :right #(set (remove #{item} %)))
  (main))

(defn root [data-state-ref]
  (let [data-state @data-state-ref]
    [:div.ui.two.column.grid.containeR
     {:join #(.. % (style "padding-top" "2em"))}

     [:div.column>:div.ui.list
      (for [item (:left data-state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true"))
                       :enter fade-in
                       :exit fade-out
                       :click #(swap-left-right data-state-ref  item)}])]
     [:div.column>:div.ui.list
      (for [item (:right data-state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true"))
                       :enter fade-in
                       :exit fade-out
                       :click #(swap-right-left data-state-ref item)}])]]))

(defn main []
  (println "***")
  (r/render
   (.. js/d3 (select "#app"))
   (root data-state)))

(main)

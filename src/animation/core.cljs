(ns animation.core
  (:require
   [cljsjs.d3]
   [cljsjs.semantic-ui]
   [animation.render :as r]))

(enable-console-print!)

(declare render swap-left-right swap-right-left fade-in fade-out)

(def data-state-atom (atom {:left
                            #{"chrome", "firefox", "edge"}
                            :right
                            #{"opera", "safari"}}))

(defn root [data-state-atom]
  (let [data-state @data-state-atom]
    [:div.ui.two.column.grid.containeR
     {:join #(.. % (style "padding-top" "2em"))}

     [:div.column>:div.ui.list
      (for [item (:left data-state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true"))
                       :enter fade-in
                       :exit fade-out
                       :click #(swap-left-right data-state-atom item)}])]
     [:div.column>:div.ui.list
      (for [item (:right data-state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true"))
                       :enter fade-in
                       :exit fade-out
                       :click #(swap-right-left data-state-atom item)}])]]))

(defn render [data-state-atom]
  (r/render
   (.. js/d3 (select "#app"))
   (root data-state-atom)))

(add-watch data-state-atom :render
           (fn [key atom _old-state _new-state]
             (render atom)))

(defn main []
  (println "***")
  (render data-state-atom))

(main)

;; state operations

(defn swap-left-right [data-state-atom item]
  (swap! data-state-atom update :right #(set (conj % item)))
  (swap! data-state-atom update :left #(set (remove #{item} %))))

(defn swap-right-left [data-state-atom item]
  (swap! data-state-atom update :left #(set (conj % item)))
  (swap! data-state-atom update :right #(set (remove #{item} %))))


;; animations

(defn fade-in [selection]
  (.. selection
      (style "opacity" 0)
      transition
      (duration 2000)
      (style "opacity" 1)))

(defn fade-out [selection]
  (.. selection
      transition
      (duration 2000)
      (style "opacity" 0)
      remove))

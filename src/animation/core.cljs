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

(defn root [state-ref]
  (let [state @state-ref]
    [:div.ui.two.column.grid.container
     {:join #(.. % (style "padding-top" "2em"))}

     [:div.column>:div.ui.list
      (for [item (:left state)]
        [:i.huge.icon {:join #(.. % (classed item "true"))
                       :click (fn []
                                (swap! state-ref update :right #(conj % item))
                                (swap! state-ref update :left #(remove #{item} %))
                                (main)
                                )
                       }])]
     [:div.column>:div.ui.list
      (for [item (:right state)]
        [:i.huge.icon {:join #(.. % (classed item "true"))
                       :click (fn []
                                (swap! state-ref update :left #(conj % item))
                                (swap! state-ref update :right #(remove #{item} %))
                                (main)
                                )
                       }])]]))

(defn main []
  (println "***")
  (r/render
   (.. js/d3 (select "#app"))
   (root state)))

(main)

(ns animation.core
  (:require
   [cljsjs.d3]
   [cljsjs.semantic-ui]
   [animation.animation :as a]
   [animation.render :as r]))

(enable-console-print!)

;; state
(def data-state-atom (atom {:left
                            #{"chrome", "firefox", "edge"}
                            :right
                            #{"opera", "safari"}}))

(def anim-state (a/state (fn [atom new-state]
                           (when (and (not (empty? new-state))
                                      (a/all-finished? new-state))
                             (reset! atom {})
                             (println "animation end")
                             )
                           )))

;; animations
(defn fade-in [selection end]
  (.. selection
      (style "opacity" 0)
      transition
      (duration 2000)
      (style "opacity" 1)
      (on "end" #(end))))

(defn fade-out [selection end]
  (.. selection
      transition
      (style "opacity" 1)
      (duration 2000)
      (style "opacity" 0)
      remove
      (on "end" #(end))))

;; state operations
(defn swap-left-right [data-state-atom item]
  (swap! data-state-atom update :right #(set (conj % item)))
  (swap! data-state-atom update :left #(set (remove #{item} %))))

(defn swap-right-left [data-state-atom item]
  (swap! data-state-atom update :left #(set (conj % item)))
  (swap! data-state-atom update :right #(set (remove #{item} %))))

;; template
(defn root [data-state-atom]
  (let [data-state @data-state-atom]
    [:div.ui.two.column.grid.containeR
     {:join #(.. % (style "padding-top" "2em"))}

     [:div.column>:div.ui.list
      (for [item (:left data-state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true"))
                       :enter #(a/add anim-state ["left enter" item] % fade-in)
                       :exit #(a/add anim-state ["left exit" item] % fade-out)
                       :click #(swap-left-right data-state-atom item)}])]
     [:div.column>:div.ui.list
      (for [item (:right data-state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true"))
                       :enter #(a/add anim-state ["right enter" item] % fade-in)
                       :exit #(a/add anim-state ["right exit" item] % fade-out)
                       :click #(swap-right-left data-state-atom item)}])]]))

;; render
(defn render [data-state-atom]
  (reset! anim-state {})

  (r/render
   (.. js/d3 (select "#app"))
   (root data-state-atom))

  (a/resolve anim-state)
  (a/play anim-state)
  )

(add-watch data-state-atom :render
           (fn [key atom _old-state _new-state]
             (render atom)))

(defn main []
  (println "***")
  (render data-state-atom))

(main)

;; state operations


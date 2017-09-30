(ns animation.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljsjs.d3]
   [cljsjs.semantic-ui]
   [cljs.core.async :refer [put! chan <! >! sliding-buffer]]
   [animation.animation :as a]
   [animation.render :as r]))

(enable-console-print!)

;; state
(def data-state-atom (atom {}))
(def anim-state-atom (atom {}))

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
                       :enter #(a/add anim-state-atom ["left enter" item] % fade-in)
                       :exit #(a/add anim-state-atom ["left exit" item] % fade-out)
                       :click #(swap-left-right data-state-atom item)}])]
     [:div.column>:div.ui.list
      (for [item (:right data-state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true"))
                       :enter #(a/add anim-state-atom ["right enter" item] % fade-in)
                       :exit #(a/add anim-state-atom ["right exit" item] % fade-out)
                       :click #(swap-right-left data-state-atom item)}])]]))

;; render
(def new-data-ch (chan (sliding-buffer 1)))
(def anim-finished-ch (chan (sliding-buffer 1)))

(defn render [data-state-atom]
  (reset! anim-state-atom {})
  (r/render
   (.. js/d3 (select "#app"))
   (root data-state-atom))

  (a/resolve anim-state-atom)
  (if (empty? @anim-state-atom)
    (put! anim-finished-ch true)
    (a/play anim-state-atom)))

(add-watch anim-state-atom :animation (fn [key atom old-state new-state]
                                        (when (a/all-finished? new-state)
                                          (println "push animation end")
                                          (put! anim-finished-ch true))))

(add-watch data-state-atom :render
           (fn [key atom old-state new-state]
             (println "push new data")
             (if (not= old-state new-state)
               (put! new-data-ch atom))))

(reset! data-state-atom {:left
                         #{"chrome", "firefox", "edge"}
                         :right
                         #{"opera", "safari"}})

(defn render-loop []
  (render data-state-atom)
  (go-loop []
    (println "*** blocking")
    (when-let [data-state-atom (<! new-data-ch)]
      (println "pop new data")
      (render data-state-atom)
      (<! anim-finished-ch)
      (println "pop animation end")
      (recur))))

(render-loop)


(ns animation.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljsjs.d3]
   [cljsjs.semantic-ui]
   [cljsjs.jquery]
   [cljs.core.async :refer [put! chan <! >! sliding-buffer]]
   [animation.animation :as a]
   [animation.render :as r]))

(enable-console-print!)

;; state
(def data-state-atom (atom {}))
(def anim-state-atom (atom {}))

;; animations
(defn pos [e]
  (let [n (.. e node)
        o (.. (js/$ n) offset)]
    [(.-top o)  (.-left o)]))

(defn icon-fly [item selections end]
  (println "fly selections" selections)
  (let [enter-icon (get selections ["left enter" item])
        exit-icon (get selections ["right exit" item])
        [t0 l0] (pos exit-icon)
        [t1 l1] (pos enter-icon)
        t       (- (- t1 t0))
        l       (- (- l1 l0))]
    (.. exit-icon
        (style "opacity" 0))
    (.. enter-icon
        (style "z-index" "2")
        (style "color" "green")
        (style "transform" (str "translate(" l "px," t "px)"))
        (transition)
        (duration 2000)
        (style "transform" "translate(0px,0px)")
        (on "end" (fn []
                    (.. exit-icon remove)
                    (println "sent end")
                    (end))))))

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
      (duration 250)
      (style "opacity" 0)
      transition
      (duration 2000)
      (style "transform" "scaleX(0)")
      (style "width" "0")
      remove
      (on "end" #(end))))

;; state operations
(defn swap-left-right [data-state-atom item]
  (swap! data-state-atom (fn [state]
                           (-> state
                               (update :right #(set (conj % item)))
                               (update :left #(set (remove #{item} %)))))))

(defn swap-right-left [data-state-atom item]
  (swap! data-state-atom (fn [state]
                           (-> state
                               (update :left #(set (conj % item)))
                               (update :right #(set (remove #{item} %)))))))

;; template
(defn root [data-state-atom]
  (let [data-state @data-state-atom]
    [:div.ui.two.column.grid.containeR
     {:join #(.. % (style "padding-top" "2em"))}

     [:div.column>:div.ui.list
      (for [item (:left data-state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true")
                                  (style "z-index" "-1"))
                       :enter (fn [sel]
                                (a/add      anim-state-atom ["left enter" item] sel fade-in)
                                (a/override anim-state-atom [["left enter" item]
                                                             ["right exit" item]] (partial icon-fly item)))
                       :exit #(a/add anim-state-atom ["left exit" item] % fade-out)
                       :click #(swap-left-right data-state-atom item)}])]
     [:div.column>:div.ui.list
      (for [item (:right data-state)]
        [:i.huge.icon {:id item
                       :join #(.. % (classed item "true")
                                  (style "z-index" "-1"))
                       :enter #(a/add anim-state-atom ["right enter" item] % fade-in)
                       :exit #(a/add anim-state-atom ["right exit" item] % fade-out)
                       :click #(swap-right-left data-state-atom item)}])]]))

;; render
(def new-data-ch (chan (sliding-buffer 1)))
(def anim-finished-ch (chan (sliding-buffer 1)))

(defn render [data-state-atom]
  (reset! anim-state-atom nil)
  (r/render
   (.. js/d3 (select "#app"))
   (root data-state-atom))

  (a/resolve anim-state-atom)
  (cond (nil? @anim-state-atom) (println "animation nil")
        (empty? @anim-state-atom) (do (println "push animatoin end on empty")
                                      (put! anim-finished-ch true))
        :default (a/play anim-state-atom)))

(add-watch anim-state-atom :animation (fn [key atom old-state new-state]
                                        (when (and (not (nil? new-state))
                                                   (a/all-finished? new-state))
                                          (println "push animation end in add-watch")
                                          (put! anim-finished-ch true))))

(add-watch data-state-atom :render
           (fn [key atom old-state new-state]
             (if (not= old-state new-state)
               (do
                 (println "push new data")
                 (put! new-data-ch atom)))))

(defn restate [left right]
  (reset! data-state-atom {:left
                           (set left);#{"chrome", "edge"}
                           :right
                           (set right)}));#{"opera", "firefox", "safari"}


(defn render-loop []
  (render data-state-atom)
  (go-loop []
    (println "*** blocking")
    (when-let
        [data-state-atom (<! new-data-ch)]
      (println "pop new data")
      (render data-state-atom)
      (<! anim-finished-ch)
      (println "pop animation end")
      (recur))))

(defonce single-loop
  (render-loop))

;; test
(restate ["chrome"] ["firefox"])
(restate ["chrome" "safari"] ["firefox" "edge"])
(restate ["chrome" "edge" "safari"] ["firefox" "opera"])
(restate ["chrome"] ["firefox"])
(restate ["chrome" "safari"] ["firefox" "edge"])
(restate ["chrome"] ["firefox"])
(restate ["chrome" "safari"] ["firefox" "edge"])
(restate ["chrome" "safari"] ["firefox" "edge"])
(restate ["chrome" "safari"] ["firefox" "edge"])
(restate ["chrome" "edge" "safari"] ["firefox" "opera"])
(restate ["chrome"] ["firefox"])
(restate ["chrome" "safari"] ["edge"])
(restate ["chrome" "edge" "safari"] ["firefox" "opera"])
(restate ["chrome"] ["firefox"])
(restate ["chrome" "safari"] ["firefox"])
(restate ["chrome" "edge" "safari"] ["firefox" "opera"])
(restate ["chrome"] ["firefox"])
(restate ["chrome" "safari"] ["firefox"])
(restate ["chrome"] ["firefox"])
(restate ["chrome" "safari"] ["firefox" "edge"])
(restate ["chrome" "edge" "safari"] ["firefox" "opera"])
(restate ["chrome"] ["firefox"])
(restate ["chrome" "firefox"] ["edge" "safari" "opera"])


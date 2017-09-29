(ns animation.core
  (:require
   [cljsjs.d3]
   [cljsjs.semantic-ui]
   [animation.render :as r]))

(enable-console-print!)

(defn root []
  [:div.ui.container
   {:join #(.. % (text "hello"))}
   ])

(defn main []
  (println "***")
  (r/render
   (.. js/d3 (select "#app"))
   (root)))

(main)

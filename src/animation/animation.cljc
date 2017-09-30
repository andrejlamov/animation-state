(ns animation.animation)

(defn add [state-ref path selection fn]
  (swap! state-ref assoc-in [:update path] {:selection selection
                                            :fn fn
                                            :state-path [:update path]}))
(defn override [state-ref paths fn]
  (swap! state-ref assoc-in [:override paths] {:fn fn
                                               :selection {}
                                               :state-path [:override paths]}))

(defn resolve [state-ref]
  (doseq [[paths animation] (:override @state-ref)]
    (doseq [path paths]
      (let [update (get (:update @state-ref) path)
            selection (:selection update)]
        (when update
          (swap! state-ref assoc-in [:override paths :selection path] selection)
          (swap! state-ref update-in [:update] dissoc path))))))

(defn flat-state [state]
  (concat (map (fn [[k v]] v) (:update state))
          (map (fn [[k v]] v) (:override state))))

(defn all-finished0? [state]
  (if (empty? state)
    false
    (every? (comp true? :finished) state)))

(defn all-finished? [state]
  (all-finished0? (flat-state state)))

(defn finished [state-ref path]
  (swap! state-ref assoc-in path true)
  (when (all-finished? @state-ref)
    ;; animate next data state or reset
    (println "finished")
    (reset! state-ref {})
    (println @state-ref)))

(defn play [state-ref]
  (doseq [anim (flat-state @state-ref)]
    (let [fun (:fn anim)
          selection (:selection anim)
          state-path (:state-path anim)
          end (fn []
                (swap! state-ref assoc-in (conj state-path :finished) true))]
      (fun selection end))))


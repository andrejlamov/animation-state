(ns animation.render
  (:require
   [cljsjs.d3]
   [clojure.string :as string]
   [cljs.core.match :refer-macros [match]]))

(defn flatten-until-children [children]
  (let [recurse
        (if (= '(()) children)
          true
          (let [[[head & _] & other] children]
            (sequential? head)))]
    (if recurse
      (flatten-until-children (first children))
      children)))

(defn build-element [tag]
  (let [[t & classes]  (string/split tag #"\.")
        class          (string/join " " classes)]
    (if (empty? class)
      [t {}]
      [t {:join #(.. % (attr "class" class))}])))

(defn merge-whatever [a b]
  (match [a b]
         [(a :guard fn?)   (b :guard fn?)]   (comp b a)
         [(a :guard coll?) (b :guard coll?)] (concat a b)
         [& _] nil))

(defn merge-props [props-a props-b]
  (let [a (:join props-a)
        b (:join props-b)
        joind (merge-whatever a b)]
    (if (nil? joind)
      (merge {} props-a props-b)
      (merge {} props-a props-b {:join joind}))))

(defn destruct-head [head props children]
  (let [[tag & tags] (reverse (string/split (name head) #"\>"))
        tags'        (map (fn [t] (build-element t)) tags)
        [tag' props'] (build-element tag)
        tag''         [tag' (merge-props props' props) children]]
    (reverse (cons tag'' tags'))))

(defn nest [tags]
  (loop [tags' (into [] (reverse
                         tags))]
    (match tags'
           [child [parent props] & other] (recur
                                           (into [] (concat [[parent props [child]]] other)))
           [it] it)))

(defn transform [thing]
  (let [[head props children]
        (match thing
               [(head :guard keyword?) (props    :guard map?)         (children :guard sequential?)] [head props [children]]
               [(head :guard keyword?) (children :guard sequential?)]                                [head {} [children]]
               [(head :guard keyword?) (props    :guard map?)         & children]                    [head props (or children [])]
               [(head :guard keyword?) & children]                                                   [head {} children])
        children' (map transform (flatten-until-children children))]
    (nest (destruct-head head props children'))))

(defn render0 [parent children]
  (let [joined  (.. parent
                    (selectAll #(this-as this
                                  (let [nodes (js/Array.from (aget this "childNodes"))]
                                    (.filter nodes (fn [n] (.-tagName n))))))
                    (data (clj->js children) (fn [d i]
                                               (let [[tag {:keys [id]} children] (js->clj d :keywordize-keys true)]
                                                 (or id (str tag "_" i))))))
        exited  (.. joined exit)

        entered  (.. joined
                     enter
                     (append
                      (fn [d i]
                        (.createElement
                         js/document
                         (let [[tag & _] d] tag)))))]
    (.. exited
        (each (fn [d]
                (this-as this
                  (let [[tag {:keys [exit]} children] (js->clj d :keywordize-keys true)
                        self                          (.. js/d3 (select this))]
                    (if exit
                      (exit self)
                      (.. self remove)))))))
    (.. entered
        (each (fn [d i]
                (this-as this
                  (let [[tag  {:keys [enter join click]} children] (js->clj d :keywordize-keys true)
                        enter (comp (or enter identity) (or join identity))
                        self  (.. js/d3 (select this) (on "click" click))]
                    (enter self)
                    (render0 self children))))))
    (.. joined
        (each (fn [d]
                (let [[tag {:keys [join click]} children] (js->clj d :keywordize-keys true)
                      draw  (or join identity)]
                  (this-as this
                    (->> (.. js/d3 (select this)
                             (on "click" click))
                         (draw)
                         (#(render0 %1 children))))))))))

(defn render [parent children]
  (render0 parent [(transform children)]))

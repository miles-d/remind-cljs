(ns remind.core
    (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "This text is printed from src/remind/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"
                          :topics #{{:title "hello"}
                                    {:title "good-bye"}}}))

;; return new state
(defn add-topic [app-state title]
  (let [old-topics (:topics app-state)
        new-topic {:title title}
        new-topics (conj old-topics new-topic)
        new-state (assoc app-state :topics new-topics)]
    new-state))

(defn add-topic! [title]
  (swap! app-state #(add-topic % title)))


(defn list-item [title]
  [:li title
   [:button "Click me!"]])

(defn remind-app []
  [:div
   [:h1 (:text @app-state)]
   [:h3 "Remind"]
   [:ol
    (for [topic (:topics @app-state)]
      ^{:key topic} [list-item (:title topic)]
      )]])

(reagent/render-component [remind-app]
                          (. js/document (getElementById "app")))

(def log (.-log js/console))

(defn on-js-reload []
  (log @app-state)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

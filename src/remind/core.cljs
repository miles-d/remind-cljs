(ns remind.core
    (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)
(def log (.-log js/console))

(defonce app-state (atom {:topics {"hello" {:last-review-date nil
                                            :review-count 0}
                                   "good-bye" {:last-review-date nil
                                               :review-count 0}}}))

(def empty-topic {:last-review-date nil :review-count 0})

(defn add-topic [app-state title]
  (assoc-in app-state [:topics title] empty-topic))

(defn add-topic! [title]
  (swap! app-state #(add-topic % title)))

(defn get-topic [app-state title]
  (get-in app-state [:topics title]))

(defn update-last-review-date [app-state topic-id now]
  (assoc-in app-state [:topics topic-id :last-review-date] now))

(defn increase-review-count [app-state topic-id]
  (update-in app-state [:topics topic-id :review-count] inc))

(defn now []
  (-> (.toISOString (js/Date.))
      (clojure.string/replace , #"[TZ]" " ")))

(defn review-topic! [topic-id]
  (swap! app-state update-last-review-date topic-id (now))
  (swap! app-state increase-review-count topic-id))

(defn reset-topic [app-state topic-id]
  (assoc-in app-state [:topics topic-id] empty-topic))

(defn reset-topic! [topic-id]
  (swap! app-state reset-topic topic-id))

(defn review-button [topic-id]
  [:button
        {:on-click #(review-topic! topic-id)}
             "Review!"])

(defn reset-button [topic-id]
  [:button
     {:on-click (fn []
                  (if (js/confirm "Are you sure to reset topic data?")
                    (reset-topic! topic-id)))}
     "Reset!"])

(defn remind-row [[topic-id topic-data]]
  [:tr
   [:td topic-id]
   [:td
    [review-button topic-id]
    [reset-button topic-id]]
   [:td (or (:last-review-date topic-data) "Never")]
   [:td (:review-count topic-data)]])

(defn remind-table []
  [:table
   [:thead
    [:tr
     [:th "Title"]
     [:th "Actions"]
     [:th "Last review"]
     [:th "Review count"]]]
   [:tbody
    (for [topic (:topics @app-state)]
      ^{:key (first topic)} [remind-row topic])]]
  )

(defn remind-app []
  [:div
   [:h3 "Remind"]
   [remind-table]])

(reagent/render-component [remind-app]
                          (. js/document (getElementById "app")))


(defn on-js-reload []
  (log @app-state)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

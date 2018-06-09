(ns remind.core
    (:require [reagent.core :as reagent :refer [atom]]
              [cljs.reader]))



(enable-console-print!)
(def log (.-log js/console))

(defonce app-state (atom (or (cljs.reader/read-string (.getItem js/localStorage "appStateV1"))
                             {:topics {}})))
(defonce timer (atom (js/Date.)))
(defonce timer-updater (js/setInterval
                         #(reset! timer (js/Date.))
                         1000))

(defn reset-state! [] (reset! app-state {:topics {}}))

(def topic-types {:vip       {:interval-days 21
                              :description "Very important people. Contact every three weeks."
                              :short-description "VIP"}
                  :important {:interval-days 60
                              :description "Important people. Contact every two months."
                              :short-description "Important"}
                  :regular   {:interval-days (* 30 6)
                              :description "Most people. Contact every six months."
                              :short-description "Regular"}
                  :demoted   {:interval-days 365
                              :description "Demoted people. Contact once a year, to make sure you still have their correct info."
                              :short-description "Demoted"}})

(add-watch app-state
           :save-to-local-storage
           (fn [_ _ _ new-state]
             (.setItem js/localStorage "appStateV1" new-state)))

(def empty-topic {:last-review-date nil
                  :review-count 0
                  :type-id nil})

(defn add-topic [app-state title type-id]
  (-> app-state
    (assoc-in , [:topics title] empty-topic)
    (assoc-in , [:topics title :type-id] type-id)))

(defn add-topic! [title type-id]
  (swap! app-state #(add-topic % title (keyword type-id))))

(defn update-last-review-date [app-state topic-id now]
  (assoc-in app-state [:topics topic-id :last-review-date] now))

(defn increase-review-count [app-state topic-id]
  (update-in app-state [:topics topic-id :review-count] inc))

(defn my-format-date [datetime]
  (if (not datetime)
    datetime ;; pass through falsey value without formatting
    (let [date (.toLocaleDateString datetime)
          time (.toLocaleTimeString datetime)]
      (str date " " time))))

(defn review-topic! [topic-id]
  (swap! app-state update-last-review-date topic-id (js/Date.))
  (swap! app-state increase-review-count topic-id))

(defn reset-topic [app-state topic-id]
  (assoc-in app-state [:topics topic-id] empty-topic))

(defn reset-topic! [topic-id]
  (swap! app-state reset-topic topic-id))

(defn delete-topic [app-state topic-id]
  (update app-state :topics dissoc topic-id))

(defn delete-topic! [topic-id]
  (swap! app-state delete-topic topic-id))

(defn topic-exists? [topic-id]
  (not (nil? (get-in @app-state [:topics topic-id]))))

(defn time-diff [time1 time2]
  (if (or (not time1)
          (not time2))
    nil
    (- (.getTime (js/Date. time1))
       (.getTime (js/Date. time2)))))

(defn add-days [date days]
  (if (nil? date)
    date
    (let [old-mili (.getTime date)
          mili-to-add (* 86400 1000 days)
          new-date (js/Date.)]
      (do
        (.setTime new-date (+ old-mili mili-to-add))
        new-date))))

(defn human-elapsed-time [date]
  (if (nil? date)
    date
    (let [seconds (quot date 1000)
          minutes (quot seconds 60)
          hours (quot minutes 60)
          days (quot hours 24)
          weeks (quot days 7)]
      (cond
        (< seconds 60) "< 1 minute"
        (= 1 minutes) (str "1 minute")
        (< minutes 120) (str minutes " minutes")
        (= hours 24) (str "1 day")
        (< hours 24) (str hours " hours")
        (< days 35) (str days " days")
        :else (str weeks " weeks")))))

(defn next-review-time [topic]
  (let [interval-days (:interval-days (get topic-types (:type-id topic)))
        next-review-absolute (add-days (:last-review-date topic) interval-days) ]
    next-review-absolute))

(defn topic-pending? [topic-data now]
  (let [next-time (next-review-time topic-data)]
    (if next-time
      (< next-time now)
      false)))

(defn sort-topics [topics]
  (let [cmp (fn [[_ topic-a] [_ topic-b]]
              (cond
                (nil? (:last-review-date topic-a)) false
                (nil? (:last-review-date topic-b)) true
                :else (let [t1 (next-review-time topic-a)
                            t2 (next-review-time topic-b)]
                        (< (.getTime t1)
                           (.getTime t2)))))]
    (sort cmp topics)))

(defn review-button [topic-id]
  [:button
   {:on-click #(review-topic! topic-id)}
   "Contacted!"])

(defn delete-button [topic-id]
  [:button
   {:on-click #(if (js/confirm "Are you sure to delete this entry?")
                 (delete-topic! topic-id))}
   "Delete"])

(defn last-review-cell [topic]
  [:td
   {:title (or (my-format-date (:last-review-date topic)) "Never reviewed")}
   (if-let [last-review-date (:last-review-date topic)]
     (str (human-elapsed-time (time-diff @timer last-review-date)) " ago")
     "-")])

(defn next-review-cell [topic]
  (let [next-review-absolute (next-review-time topic)
        diff-mili (time-diff next-review-absolute @timer)]
    [:td
     {:title (or (my-format-date next-review-absolute))}
     (cond
       (< diff-mili 0) "Pending!"
       (not next-review-absolute) "-"
       :else (str "in " (human-elapsed-time diff-mili)))]))

(defn remind-row [[topic-id topic-data]]
  [:tr
   {:class (when (topic-pending? topic-data (js/Date.)) "pending")}
   [:td.title-column topic-id]
   [:td (or (:short-description (get topic-types (:type-id topic-data)))
            "")]
   [:td
    [review-button topic-id]
    [delete-button topic-id]]
   [last-review-cell topic-data]
   [next-review-cell topic-data]
   [:td.review-count-column (:review-count topic-data)]])

(defn remind-table []
  [:table#topics-table
   [:thead
    [:tr
     [:th.title-column "Name"]
     [:th "Type"]
     [:th "Actions"]
     [:th.last-review-column "Last contact"]
     [:th "Next contact"]
     [:th.review-count-column "Times contacted"]]]
   [:tbody
    (for [topic (sort-topics (:topics @app-state))]
      ^{:key (first topic)} [remind-row topic])]]
  )

(defn new-topic-input []
  (let [input-value (atom "")
        error-message (atom "")
        type-id (atom :vip)]
    (fn []
      [:form#new-topic-form
       [:label "Name: "
        [:input {:value @input-value
                 :on-change (fn [event]
                              (let [new-value (.-value (.-target event))]
                                (reset! error-message "")
                                (reset! input-value new-value)))}]]
       [:br]
       [:label "Type: "
        [:select#topic-type-select
         {:value @type-id
          :on-change (fn [event]
                       (reset! type-id (.-value (.-target event))))}
         (for [[type-id topic-type] topic-types]
           ^{:key type-id} [:option {:value type-id} (:description topic-type)])]]
       [:br]
       [:button {:on-click (fn [event]
                             (do
                               (.preventDefault event)
                               (cond
                                 (clojure.string/blank? @input-value) (reset! error-message "Cannot add empty topic.")
                                 (topic-exists? @input-value) (reset! error-message "Topic with this title already exists!")
                                 :else (do
                                         (add-topic! (clojure.string/trim @input-value) @type-id)
                                         (reset! input-value "")))))}
        "Add!"]
       [:span#new-topic-error-message @error-message]])))

(defn remind-app []
  [:div
   [:h3 "Stay In Touch"]
   [new-topic-input]
   [remind-table]])

(reagent/render-component [remind-app]
                          (. js/document (getElementById "app")))


(defn on-js-reload []
  ; (log @app-state)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(ns ^:figwheel-always remind.core-test
  (:require  [cljs.test :refer-macros  [deftest is testing run-tests]]
            [remind.core :refer [sort-topics add-topic add-topic topic-pending?]]))

(deftest test-foo
  (is (= true true)))

(deftest sort-topics-test
  (testing "Sorts topics"
    (let [older-topic  {:type-id :vip :last-review-date (js/Date. "2018-01-01")}
          medium-topic {:type-id :vip :last-review-date (js/Date. "2018-01-02")}
          newer-topic  {:type-id :vip :last-review-date (js/Date. "2018-01-03")}]
      (is
        (= [["old" older-topic]
            ["medium" medium-topic]
            ["new" newer-topic]]
           (sort-topics {"medium" medium-topic
                         "new" newer-topic
                         "old" older-topic})))))

  (testing "puts topics without last-review-date as first"
    (let [topic-without-date {:type-id :vip :last-review-date nil}
          topic-with-date    {:type-id :vip :last-review-date (js/Date. "2018-01-01")}]
      (is
        (= [["without date" topic-without-date]
            ["with date" topic-with-date]]
           (sort-topics {"without date" topic-without-date
                         "with date" topic-with-date}))))))

(deftest topic-pending?-test
  (testing "tells if topic should be reviewed"
    (let [now (js/Date. "2018-01-01")
          pending-topic {:type-id :vip :last-review-date (js/Date. "2017-01-01")}
          not-pending-topic {:type-id :vip :last-review-date (js/Date. "2018-01-01")}
          topic-with-empty-date {:type-id :vip :last-review-date nil}]
      (is (= true (topic-pending? pending-topic now)))
      (is (= false (topic-pending? not-pending-topic now)))
      (is (= true (topic-pending? topic-with-empty-date now))))))

(run-tests)

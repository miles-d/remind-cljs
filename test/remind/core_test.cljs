(ns ^:figwheel-always remind.core-test
  (:require  [cljs.test :refer-macros  [deftest is testing run-tests]]
            [remind.core :refer [sort-topics add-topic add-topic]]))

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

  (testing "puts topics without last-review-date as last"
    (let [topic-without-date {:type-id :vip :last-review-date nil}
          topic-with-date    {:type-id :vip :last-review-date (js/Date. "2018-01-01")}]
      (is
        (= [["with date" topic-with-date]
            ["without date" topic-without-date]]
           (sort-topics {"without date" topic-without-date
                         "with date" topic-with-date}))))))

(run-tests)

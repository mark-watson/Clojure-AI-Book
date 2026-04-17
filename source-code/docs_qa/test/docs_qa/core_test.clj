(ns docs-qa.core-test
  (:require [clojure.test :refer :all]
            [docs-qa.core :refer :all]
            [docs-qa.vectordb :refer :all]
            [openai-api.core :refer :all]))

(deftest a-test
  (testing "Test best-vector-matches with mocked OpenAI API"
    (with-redefs [openai-api.core/embeddings (fn [_] (repeat 1536 0.1))
                  openai-api.core/dot-product (fn [_ _] 0.85)
                  openai-api.core/answer-question (fn [_] "Mocked answer")]
      (let [matches (docs-qa.core/best-vector-matches "test query")]
        (println matches)
        (is (string? matches))
        (is (not (empty? matches)))))))

(ns ollama-api.core-test
  (:require [clojure.test :refer :all]
            [ollama-api.core :as ollama]))

(def some-text
  "Jupiter is the fifth planet from the Sun and the largest in the Solar System. It is a gas giant with a mass one-thousandth that of the Sun, but two-and-a-half times that of all the other planets in the Solar System combined. Jupiter is one of the brightest objects visible to the naked eye in the night sky, and has been known to ancient civilizations since before recorded history. It is named after the Roman god Jupiter.[19] When viewed from Earth, Jupiter can be bright enough for its reflected light to cast visible shadows,[20] and is on average the third-brightest natural object in the night sky after the Moon and Venus.")

(deftest completions-test
  (testing "ollama completions API"
    (let [results (ollama/completions "He walked to the river")]
      (println results)
      (is (string? results))
      (is (not-empty results)))))

(deftest summarize-test
  (testing "ollama summarize API"
    (let [results (ollama/summarize some-text)]
      (println results)
      (is (string? results))
      (is (not-empty results)))))

(deftest question-answering-test
  (testing "ollama question-answering API"
    (let [results (ollama/answer-question "Where is the Valley of Kings?")]
      (println results)
      (is (string? results))
      (is (not-empty results)))))

(deftest chat-test
  (testing "ollama chat API"
    (let [messages [{:role "user" :content "Hello, who are you?"}]
          results (ollama/chat messages)]
      (println results)
      (is (string? results))
      (is (not-empty results)))))

(deftest mock-completions-test
  (testing "Internal logic with mocked HTTP"
    (with-redefs [clj-http.client/post (fn [_ _]
                                        {:status 200
                                         :body "{\"response\": \"Mocked result\"}"})]
      (is (= "Mocked result" (ollama/completions "test prompt"))))))

(deftest error-handling-test
  (testing "Error handling when API fails"
    (with-redefs [clj-http.client/post (fn [_ _]
                                        {:status 500
                                         :body "Internal Server Error"})]
      (is (thrown? Exception (ollama/completions "test prompt"))))))

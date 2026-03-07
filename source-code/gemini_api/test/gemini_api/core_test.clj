(ns gemini-api.core-test
  (:require [clojure.test :refer :all]
            [gemini-api.core :refer :all]))

(def some-text
  "Jupiter is the fifth planet from the Sun and the largest in the Solar System. It is a gas giant with a mass one-thousandth that of the Sun, but two-and-a-half times that of all the other planets in the Solar System combined. Jupiter is one of the brightest objects visible to the naked eye in the night sky, and has been known to ancient civilizations since before recorded history. It is named after the Roman god Jupiter.[19] When viewed from Earth, Jupiter can be bright enough for its reflected light to cast visible shadows,[20] and is on average the third-brightest natural object in the night sky after the Moon and Venus.")

(deftest completions-test
  (testing "gemini completions API"
    (let [results
          (gemini-api.core/generate-content "He walked to the river")]
      (println results)
      (is (= 0 0)))))

(deftest summarize-test
  (testing "gemini summarize API"
    (let [results
          (gemini-api.core/summarize
           some-text)]
      (println results)
      (is (= 0 0)))))

(deftest question-answering-test
  (testing "gemini question-answering API"
    (let [results
          (gemini-api.core/generate-content
            ;;"If it is not used for hair, a round brush is an example of what 1. hair brush 2. bathroom 3. art supplies 4. shower ?"
           "Where is the Valley of Kings?"
            ;"Where is San Francisco?"
           )]
      (println results)
      (is (= 0 0)))))

;; ── Google Search (grounding) ─────────────────────────────────────────────────

(deftest search-test
  (testing "gemini Google Search grounding API"
    (let [[text citations]
          (gemini-api.core/generate-with-search-and-citations
           "Who wrote the Clojure programming language?")]
      (println "Search result text:" text)
      (println "Citations:")
      (doseq [{:keys [title uri]} citations]
        (println " -" title uri))
      ;; text should be a non-empty string
      (is (string? text))
      (is (pos? (count text))))))

;; ── Tool / Function calling ───────────────────────────────────────────────────

(defn- mock-get-weather
  "Fake weather lookup – no real HTTP call; just returns a canned string."
  [{:keys [location]}]
  (str "It is 72°F and sunny in " location "."))

(def ^:private weather-tool-def
  {:name        "get_weather"
   :description "Returns current weather conditions for a given city."
   :parameters  {:type       "OBJECT"
                 :properties {:location {:type        "STRING"
                                         :description "The name of the city."}}
                 :required   ["location"]}})

(deftest tool-use-test
  (testing "gemini function/tool calling API"
    (let [result
          (gemini-api.core/generate-with-tools
           "What is the weather like in Paris right now?"
           [weather-tool-def]
           {"get_weather" mock-get-weather})]
      (println "Tool-use result:" result)
      ;; The model should have issued a functionCall; our dispatch fn returns
      ;; a string describing the weather.
      (is (map? result))
      (is (or (contains? result :text)
              (contains? result :function-call)))
      (when (contains? result :function-call)
        (is (string? (:result result)))
        (is (re-find #"Paris" (:result result)))))))
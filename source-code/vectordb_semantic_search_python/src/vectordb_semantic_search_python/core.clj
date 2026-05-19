(ns vectordb-semantic-search-python.core
  (:require [libpython-clj2.require :refer [require-python]]
            [libpython-clj2.python :as py :refer [py. py.-]]))

;; Initialize Python pointing to our local uv venv Python executable
(py/initialize! :python-executable
                (str (System/getProperty "user.dir") "/.venv/bin/python"))

;; Load our local wrapper vector_db.py
(require-python '[vector_db :as db])

(defn add-documents!
  "Adds documents to a collection. documents, metadatas, and ids should be Clojure lists/vectors."
  [collection-name documents metadatas ids]
  (db/add_documents collection-name documents metadatas ids))

(defn query-documents
  "Queries a collection for similarity. Returns a Clojure data structure containing matching documents."
  [collection-name query-text n-results]
  (db/query_documents collection-name query-text n-results))

(defn -main
  [& _]
  (println "=== Starting Vector DB Semantic Search Demo ===")
  (let [collection "clojure_ai_docs"
        docs ["Clojure is a modern Lisp dialect that targets the JVM, CLR, and JavaScript. It features functional programming and immutable data structures."
              "Python is an interpreted, high-level, general-purpose programming language. It is widely used in data science, AI, and machine learning."
              "French onion soup is a soup made from onions, beef stock, and usually served with cheese and bread."
              "Generative adversarial networks (GANs) are a class of machine learning frameworks where two neural networks contest with each other in a game."]
        metadatas [{"type" "programming" "lang" "clojure"}
                   {"type" "programming" "lang" "python"}
                   {"type" "food" "cuisine" "french"}
                   {"type" "ai" "topic" "gan"}]
        ids ["id_clojure" "id_python" "id_soup" "id_gan"]]
    
    (println "Inserting sample documents...")
    (add-documents! collection docs metadatas ids)
    (println "Documents inserted successfully.")
    
    (println "\n--- Running semantic search query 1: 'neural network architectures' ---")
    (let [results (query-documents collection "neural network architectures" 2)]
      (doseq [res results]
        (println "Match:" (:document res))
        (println "Metadata:" (:metadata res))
        (println "Distance:" (:distance res) "\n")))

    (println "--- Running semantic search query 2: 'functional programming language' ---")
    (let [results (query-documents collection "functional programming language" 1)]
      (doseq [res results]
        (println "Match:" (:document res))
        (println "Metadata:" (:metadata res))
        (println "Distance:" (:distance res) "\n")))

    (println "--- Running semantic search query 3: 'cooking recipes and food' ---")
    (let [results (query-documents collection "cooking recipes and food" 1)]
      (doseq [res results]
        (println "Match:" (:document res))
        (println "Metadata:" (:metadata res))
        (println "Distance:" (:distance res) "\n")))
    (shutdown-agents)))

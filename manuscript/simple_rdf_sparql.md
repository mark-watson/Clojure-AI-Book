# Simple RDF Datastore and Partial SPARQL Query Processor

In this chapter, we'll explore how to build a basic RDF (Resource Description Framework) datastore and implement a partial SPARQL (SPARQL Protocol and RDF Query Language) query processor using Clojure. The goal is to provide a simple but effective demonstration of RDF data manipulation and querying in a functional programming context.

The Clojure code for this example can be found at [https://github.com/mark-watson/Clojure-AI-Book-Code/tree/main/simple_rdf_sparql](https://github.com/mark-watson/Clojure-AI-Book-Code/tree/main/simple_rdf_sparql).

RDF is a widely-used standard for representing knowledge graphs and linked data, which makes it a valuable tool for applications that need to model complex relationships between entities. SPARQL is the accompanying query language designed to extract and manipulate RDF data, similar to how SQL works with relational databases.

This chapter will cover:

- Implementing a simple RDF datastore.
- Managing RDF triples (adding, removing, and querying).
- Designing a partial SPARQL query processor to execute basic pattern matching.
- Running example queries to demonstrate functionality.

By the end of this chapter, you'll have a good grasp of how to handle RDF data and implement a lightweight SPARQL engine that can process simple queries.

## Implementing a Simple RDF Datastore

Let’s begin by creating a simple in-memory RDF datastore using Clojure. An RDF triple is a fundamental data structure composed of a subject, predicate, and object. We will define a Triple record to represent these triples and store them in an atom.

```clojure
(ns simple-rdf-sparql.core
  (:require [clojure.string :as str]))

;; RDF triple structure
(defrecord Triple [subject predicate object])

;; RDF datastore
(def ^:dynamic *rdf-store* (atom []))
```

With our triple structure defined, we can implement functions to add and remove triples from the datastore:

```clojure
;; Add a triple to the datastore
(defn add-triple [subject predicate object]
  (swap! *rdf-store* conj (->Triple subject predicate object)))

;; Remove a triple from the datastore
(defn remove-triple [subject predicate object]
  (swap! *rdf-store* (fn [store]
                       (remove #(and (= (:subject %) subject)
                                     (= (:predicate %) predicate)
                                     (= (:object %) object))
                               store))))
```

## Querying the RDF Datastore

Next, we need a way to query the datastore to find specific triples. We’ll start by defining a helper function to filter triples that match a given pattern:

```clojure
;; Helper function to check if a string is a variable
(defn variable? [s]
  (and (string? s) (not (empty? s)) (= (first s) \?)))

;; Convert triple to binding
(defn triple-to-binding [triple pattern]
  (into {}
        (filter second
                (map (fn [field pattern-item]
                       (when (variable? pattern-item)
                         [pattern-item (field triple)]))
                     [:subject :predicate :object]
                     pattern))))

(defn query-triples [subject predicate object]
  (filter (fn [triple]
            (and (or (nil? subject) (variable? subject)
                     (= (:subject triple) subject))
                 (or (nil? predicate) (variable? predicate)
                     (= (:predicate triple) predicate))
                 (or (nil? object) (variable? object)
                     (= (:object triple) object))))
          @*rdf-store*))
```

This code allows us to extract specific triples from the RDF datastore using pattern matching. Variables in a query pattern are denoted by a **?** prefix.

## Implementing a Partial SPARQL Query Processor

Now, let's implement a basic SPARQL query processor. We’ll define a simple query structure and create functions to parse and execute these queries. We need to parse SPARQL queries like:

    select * where { ?name age ?age . ?name likes ?food }

```clojure
;; SPARQL query structure
(defrecord SPARQLQuery [select-vars where-patterns])

;; Parse a SPARQL query string
(defn parse-sparql-query [query-string]
  (let [tokens (remove #{"{"  "}"}
                       (str/split query-string #"\s+"))
        select-index (.indexOf tokens "select")
        where-index (.indexOf tokens "where")
        select-vars (subvec (vec tokens)
                            (inc select-index) where-index)
        where-clause (subvec (vec tokens)
                             (inc where-index))
        where-patterns
        (parse-where-patterns where-clause)]
    (->SPARQLQuery select-vars where-patterns)))
```

Next, we’ll define functions to execute the WHERE patterns in a SPARQL query:

```clojure
;; Execute WHERE patterns with bindings
(defn execute-where-patterns-with-bindings [patterns bindings]
  (if (empty? patterns)
    [bindings]
    (let [pattern (first patterns)
          remaining-patterns (rest patterns)
          bound-pattern (apply-bindings pattern bindings)
          matching-triples (apply query-triples bound-pattern)
          new-bindings (map #(merge-bindings bindings (triple-to-binding % pattern))
                            matching-triples)]
      (if (empty? remaining-patterns)
        new-bindings
        (mapcat #(execute-where-patterns-with-bindings remaining-patterns %)
                new-bindings)))))

(defn execute-where-patterns [patterns]
  (if (empty? patterns)
    [{}]
    (let [pattern (first patterns)
          remaining-patterns (rest patterns)
          matching-triples (apply query-triples pattern)
          bindings (map #(triple-to-binding % pattern) matching-triples)]
      (if (empty? remaining-patterns)
        bindings
        (mapcat (fn [binding]
                  (let [results
                        (execute-where-patterns-with-bindings
                            remaining-patterns binding)]
                    (map #(merge-bindings binding %) results)))
                bindings)))))
```

## Putting It All Together: Running Example Queries

Finally, let’s test our partial SPARQL query processor with some example queries. First, we’ll populate the datastore with a few RDF triples:

```clojure
(defn -main []
  (reset! *rdf-store* [])

  (add-triple "John" "age" "30")
  (add-triple "John" "likes" "pizza")
  (add-triple "Mary" "age" "25")
  (add-triple "Mary" "likes" "sushi")
  (add-triple "Bob" "age" "35")
  (add-triple "Bob" "likes" "burger")
```

Next we print all triples in the datastore and execute three sample SPARQL queries:

```clojure
  (print-all-triples)
  (print-query-results
    "select * where { ?name age ?age . ?name likes ?food }")
  (print-query-results
    "select ?s ?o where { ?s likes ?o }")
  (print-query-results
    "select * where { ?name age ?age . ?name likes pizza }"))
```

## Summary

This chapter demonstrated a minimalistic RDF datastore and a partial SPARQL query processor. We built the foundation to manage RDF triples and run basic pattern-based queries. This simple example can serve as a minimal embedded RDF data store for larger applications or as springboard for more advanced features like full SPARQL support, optimization techniques, and complex query structures. I hope, dear reader, that you have fun with this example.


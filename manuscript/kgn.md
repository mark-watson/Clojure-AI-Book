# Knowledge Graph Navigator {#kgn}


The Knowledge Graph Navigator (which I will often refer to as KGN) is a tool for processing a set of entity names and automatically exploring the public Knowledge Graph [DBPedia](http://dbpedia.org) using SPARQL queries. I started to write KGN for my own use, to automate some things I used to do manually when exploring Knowledge Graphs, and later thought that KGN might be also useful for educational purposes. KGN shows the user the auto-generated SPARQL queries so hopefully the user will learn by seeing examples. KGN uses code developed in the earlier chapter [Resolve Entity Names to DBPedia References](#ner) and we will reuse here as well as the two Java classes **JenaAPis** and **QueryResults** (which wrap the Apache Jena library) from the chapter [Semantic Web](#semantic-web).

I have a [web site devoted to different versions of KGN](http://www.knowledgegraphnavigator.com/) that you might find interesting. The most full featured version of KGN, including a full user interface, is featured in my book [Loving Common Lisp, or the Savvy Programmer's Secret Weapon](https://leanpub.com/lovinglisp) that you can read free online. That version performs more speculative SPARQL queries to find information compared to the example here that I designed for ease of understanding, modification, and embedding in larger Java projects.

I chose to use DBPedia instead of WikiData for this example because DBPedia URIs are human readable. The following URIs represent the concept of a *person*. The semantic meanings of DBPedia and FOAF (friend of a friend) URIs are self-evident to a human reader while the WikiData URI is not:

{linenos=off}
~~~~~~~~
http://www.wikidata.org/entity/Q215627
http://dbpedia.org/ontology/Person
http://xmlns.com/foaf/0.1/name
~~~~~~~~

I frequently use WikiData in my work and WikiData is one of the most useful public knowledge bases. I have both DBPedia and WikiData Sparql endpoints in the file **Sparql.java** that we will look at later, with the WikiData endpoint comment out. You can try manually querying WikiData at the [WikiData SPARL endpoint](https://query.wikidata.org). For example, you might explore the WikiData URI for the *person* concept using:

{lang=sparql, linenos=off}
~~~~~~~~
select ?p ?o where { <http://www.wikidata.org/entity/Q215627> ?p ?o  } limit 10
~~~~~~~~

For the rest of this chapter we will just use DBPedia.

After looking an interactive session using the example program for this chapter (that also includes listing automatically generated SPARQL queries) we will look at the implementation.

## Entity Types Handled by KGN

To keep this example simple we handle just four entity types:

- People
- Organizations
- Places
 
In addition to finding detailed information for people, companies, cities, and countries we will also search for relationships between person entities and company entities. This search process consists of generating a series of SPARQL queries and calling the DBPedia SPARQL endpoint.

As we look at the KGN implementation I will point out where and how you can easily add support for more entity types and in the wrap-up I will suggest further projects that you might want to try implementing with this example.

Before we design and write the code, I want to show you the final output for an example:

```
(kgn {:People ["Bill Gates" "Steve Jobs" "Melinda Gates"]
      :Organization ["Microsoft"]
      :Place        ["California"]})
```

The output (with some text shortened) is:

```
{:entity-summaries
 (("Bill Gates"
   "http://dbpedia.org/resource/Bill_Gates"
   "William Henry Gates III (born October 28, 1955) is an American business magnate, software developer, investor, and philanthropist. He is best known as the co-founder of Microsoft Corporation. During his career...")
  ("Steve Jobs"
   "http://dbpedia.org/resource/Steve_Jobs"
   "Steven Paul Jobs (; February 24, 1955 – October 5, 2011) was an American business magnate, industrial designer, investor, and media proprietor. He was the chairman, chief executive officer (CEO), and co-founder of Apple Inc., the chairman and majority shareholder of Pixar...")
  ("Melinda Gates"
   "http://dbpedia.org/resource/Melinda_Gates"
   "Melinda Ann Gates (née French; August 15, 1964) is an American philanthropist and a former general manager at Microsoft. In 2000, she co-founded the Bill & Melinda Gates Foundation with her husband Bill Gates...")
  ("Microsoft"
   "http://dbpedia.org/resource/Microsoft"
   "Microsoft Corporation () is an American multinational technology company with headquarters in Redmond, Washington. It develops, manufactures, licenses, supports, and sells computer software...")
  ("California"
   "http://dbpedia.org/resource/California"
   "California is a state in the Pacific Region of the United States. With 39.5 million residents across ...")),
 :discovered-relationships
 ((["<http://dbpedia.org/resource/Bill_Gates>"
    "<http://dbpedia.org/property/spouse>"
    "<http://dbpedia.org/resource/Melinda_Gates>"]
   ["<http://dbpedia.org/resource/Melinda_Gates>"
    "<http://dbpedia.org/property/spouse>"
    "<http://dbpedia.org/resource/Bill_Gates>"])
  (["<http://dbpedia.org/resource/Bill_Gates>"
    "<http://dbpedia.org/ontology/knownFor>"
    "<http://dbpedia.org/resource/Microsoft>"]
   ["<http://dbpedia.org/resource/Microsoft>"
    "<http://dbpedia.org/property/founders>"
    "<http://dbpedia.org/resource/Bill_Gates>"])
  (["<http://dbpedia.org/resource/Steve_Jobs>"
    "<http://dbpedia.org/ontology/birthPlace>"
    "<http://dbpedia.org/resource/California>"]))}
```

## General Design of KGN with Example Output

The example application works processing a list or Person, Place, and Organization names. We fgenerate SPARQL queries to DBPedia to find information about the entities and relationships between them.

Since the DBPedia queries are time consuming, I created a tiny subset of DBPedia in the file **dbpedia_sample.nt** and load it into a RDF data store like **GraphDB** or **Fuseki** running on my laptop. This local setup is especially helpful during development when the same queries are repeatedly used for testing.


## Implementation

TBD

{lang="Clojure",linenos=on}
~~~~~~~~
(defproject knowledge_graph_navigator_clj "0.1.0-SNAPSHOT"
  :description "Knowledge Graph Navigator"
  :url "https://markwatson.com"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-http "3.10.3"]
                 [com.cemerick/url "0.1.1"]
                 [org.clojure/data.csv "1.0.0"]
                 [org.clojure/data.json "1.0.0"]
                 [org.clojure/math.combinatorics "0.1.6"]]
  :repl-options {:init-ns knowledge-graph-navigator-clj.kgn}
  :main ^:skip-aot knowledge-graph-navigator-clj.kgn)
~~~~~~~~


**sparql.clj**:

{lang="Clojure",linenos=on}
~~~~~~~~
(ns knowledge-graph-navigator-clj.sparql
  (:require [clj-http.client :as client])
  (:require clojure.stacktrace)
  (:require [cemerick.url :refer (url-encode)])
  (:require [clojure.data.csv :as csv]))

;; Copied from https://github.com/mark-watson/clj-sparql

(def USE-LOCAL-GRAPHDB true)

(defn dbpedia [sparql-query]
  ;;(let [q (str "https://dbpedia.org//sparql?output=csv&query=" (url-encode sparql-query))
  (let [q (str "http://127.0.0.1:8080/sparql?output=csv&query=" (url-encode sparql-query))
        _ (println q)
        response (client/get q)
        body (:body response)]
    (csv/read-csv body)))

(defn- graphdb-helper [host port graph-name sparql-query]
  (let [q (str host ":" port "/repositories/" graph-name "?query=" (url-encode sparql-query))
        response (client/get q)
        body (:body response)]
    (csv/read-csv body)))

(defn graphdb
  ([graph-name sparql-query] (graphdb-helper "http://127.0.0.1" 7200 graph-name sparql-query))
  ([host port graph-name sparql-query] (graphdb-helper host port graph-name sparql-query)))

(defn sparql-endpoint [sparql-query]
  (try
    (if USE-LOCAL-GRAPHDB
      ;;(graphdb "dbpedia" sparql-query)
      (dbpedia sparql-query))
    (catch Exception e
      (do
        (println "WARNING: a SPARQL query failed:\n" sparql-query)
        (println (.getMessage e))
        (clojure.stacktrace/print-stack-trace e)
        []))))

(defn -main
  "SPARQL example"
  [& _]
  (println (sparql-endpoint "select * { ?s ?p ?o } limit 10")))
~~~~~~~~


**entities_by_name.clj**:

{lang="Clojure",linenos=on}
~~~~~~~~
(ns knowledge-graph-navigator-clj.entities-by-name
  (:require [knowledge-graph-navigator-clj.sparql :as sparql])
  (:require [clojure.pprint :as pp])
  (:require clojure.string))

(defn dbpedia-get-entities-by-name [name dbpedia-type]
  ;(println "** dbpedia-get-entities-by-name: name=" name "dbpedia-type=" dbpedia-type)
  (let [sparql-query
        (clojure.string/join
          ""
          ["select distinct ?s ?comment where { ?s <http://www.w3.org/2000/01/rdf-schema#label> \""
           name
           "\"@en . ?s <http://www.w3.org/2000/01/rdf-schema#comment>  ?comment  . FILTER  (lang(?comment) = \"en\") . ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
           dbpedia-type
           ". }"])
        results (sparql/sparql-endpoint sparql-query)]
    ;(println "Generated SPARQL to get DBPedia entity URIs from a name:")
    ;(println (colorize/colorize-sparql sparql-query))
    ;(println "Results:") (pprint results)
    results))

(defn -main
  "I don't do a whole lot."
  [& _]
  (println (dbpedia-get-entities-by-name "Steve Jobs" "<http://dbpedia.org/ontology/Person>"))
  (println (dbpedia-get-entities-by-name "Microsoft" "<http://dbpedia.org/ontology/Organization>"))
  (pp/pprint (dbpedia-get-entities-by-name "California" "<http://dbpedia.org/ontology/Place>"))
  )
~~~~~~~~


**relationships.clj**:

{lang="Clojure",linenos=on}
~~~~~~~~
(ns knowledge-graph-navigator-clj.relationships
  (:require [knowledge-graph-navigator-clj.sparql :as sparql]) ;; for non-cached
  (:require [clojure.pprint :as pp])
  (:require clojure.string))

(defn dbpedia-get-relationships [s-uri o-uri]
  (let [query
        (clojure.string/join
          ""
          ["SELECT DISTINCT ?p {{  "
           s-uri " ?p " o-uri " . FILTER (!regex(str(?p), \"wikiPage\", \"i\")) }} LIMIT 5"])
        results (sparql/sparql-endpoint query)]
    (map
      (fn [u] (clojure.string/join "" ["<" u ">"]))
      (second results))))                                   ; discard SPARQL variable name p (?p)

(defn entity-results->relationship-links [uris-no-brackets]
  (let [uris (map
               (fn [u] (clojure.string/join "" ["<" u ">"]))
               uris-no-brackets)
        relationship-statements (atom [])]
    (doseq [e1 uris]
      (doseq [e2 uris]
        (if (not (= e1 e2))
          (let [l1 (dbpedia-get-relationships e1 e2)
                l2 (dbpedia-get-relationships e2 e1)]
            (doseq [x l1]
              (let [a-tuple [e1 x e2]]
                (if (not (. @relationship-statements contains a-tuple))
                  (reset! relationship-statements (cons a-tuple @relationship-statements))
                  nil))
            (doseq [x l2]
              (let [a-tuple [e2 x e1]]
                (if (not (. @relationship-statements contains a-tuple))
                  (reset! relationship-statements (cons a-tuple @relationship-statements))
                  nil)))))
          nil)))
    @relationship-statements))

(defn -main
  "I don't do a whole lot."
  [& _]
  (println "Testing entity-results->relationship-links")
  (pp/pprint (entity-results->relationship-links ["http://dbpedia.org/resource/Bill_Gates" "http://dbpedia.org/resource/Microsoft"])))

~~~~~~~~

**kgn.clj**:

{lang="Clojure",linenos=on}
~~~~~~~~
(ns knowledge-graph-navigator-clj.kgn
  (:require [knowledge-graph-navigator-clj.entities-by-name :as entity-name])
  (:require [knowledge-graph-navigator-clj.relationships :as rel])
  (:require [clojure.math.combinatorics :as combo])
  (:require [clojure.pprint :as pp]))

(def entity-map {:People       "<http://dbpedia.org/ontology/Person>"
                 :Organization "<http://dbpedia.org/ontology/Organization>"
                 :Place        "<http://dbpedia.org/ontology/Place>"})

(defn kgn
  "Top level function for the Knowledge Graph Navigator library
   Inputs: a map with keys Person, Place, and Organization. values list of names"
  [input-entity-map]
  ;;(println "* kgn:" input-entity-map)
  (let [entities-summary-data
        (filter
          (fn [x] (> (count x) 1))  ;; get rid of empty SPARQL results
          (mapcat                   ;; flatten just top level
            identity
            (for [entity-key (keys input-entity-map)]
              (for [entity-name (input-entity-map entity-key)]
                (cons
                  entity-name
                  (second
                    (entity-name/dbpedia-get-entities-by-name 
                     entity-name
                     (entity-map entity-key))))))))
        entity-uris (map second entities-summary-data)
        combinations-by-2-of-entity-uris (combo/combinations entity-uris 2)
        discovered-relationships
        (filter
          (fn [x] (> (count x) 0))
          (for [pair-of-uris combinations-by-2-of-entity-uris]
            (seq (rel/entity-results->relationship-links pair-of-uris))))]
    {:entity-summaries entities-summary-data
     :discovered-relationships discovered-relationships}))

(defn -main
  "Main function for KGN example"
  [& _]
  (let [results (kgn {:People       ["Bill Gates" "Steve Jobs" "Melinda Gates"]
                      :Organization ["Microsoft"]
                      :Place        ["California"]})]
    (println " -- results:") (pp/pprint results)))
~~~~~~~~



This KGN example was hopefully both interesting to you and simple enough in its implementation (because we relied heavily on code from the last two chapters) that you feel comfortable modifying it and reusing it as a part of your own Java applications.


## Wrap-up

If you enjoyed running and experimenting with this example and want to modify it for your own projects then I hope that I provided a sufficient road map for you to do so.

I suggest further projects that you might want to try implementing with this example:

- Write a web application that processes news stories and annotates them with additional data from DBPedia and/or WikiData.
- In a web or desktop application, detect entities in text and display additional information when the user's mouse cursor hovers over a word or phrase that is identified as an entity found in DBPedia or WikiData.
- Clone this KGN example and enable it to work simultaneously with DBPedia, WikiData, and local RDF files by using three instances of the class **JenaApis** and in the main application loop access all three data sources.

I had the idea for the KGN application because I was spending quite a bit of time manually setting up SPARQL queries for DBPedia (and other public sources like WikiData) and I wanted to experiment with partially automating this process. I have experimented with versions of KGN written in Java, Hy language ([Lisp running on Python that I wrote a short book on](https://leanpub.com/hy-lisp-python/read)), Swift, and Common Lisp and all four implementations take different approaches as I experimented with different ideas. You might want to check out my [web site devoted to different versions of KGN: www.knowledgegraphnavigator.com](http://www.knowledgegraphnavigator.com/).

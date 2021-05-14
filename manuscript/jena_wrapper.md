# Clojure Wrapper for the Jena RDF and SPARQL Library

TBD

We declare both Jena and the Derby relational database libraries as dependencies in out project file:

{lang="clojure",linenos=on}
~~~~~~~~
(defproject semantic_web_jena_clj "0.1.0-SNAPSHOT"
  :description "Clojure Wrapper for Apache Jena"
  :url "https://markwatson.com"
  :license
  {:name "EPL-2.0 OR GPL-2+ WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}
  :source-paths      ["src"]
  :java-source-paths ["src-java"]
  :javac-options     ["-target" "1.8" "-source" "1.8"]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.apache.derby/derby "10.15.2.0"]
                 [org.apache.derby/derbytools "10.15.2.0"]
                 [org.apache.derby/derbyclient
                  "10.15.2.0"]
                 [org.apache.jena/apache-jena-libs
                  "3.17.0" :extension "pom"]]
  :repl-options {:init-ns semantic-web-jena-clj.core})
~~~~~~~~

We will use the Jena library for handling RDF and SPARQL queries and the Derby database library for implementing query caching. It is worth noting the directory structure for this project that also includes Java code (some files not shown for brevity):

{linenos=on}
~~~~~~~~
$ tree                
.
├── LICENSE
├── README.md
├── data
│   ├── business.sql
│   ├── news.nt
│   ├── sample_news.nt
├── pom.xml
├── project.clj
├── src
│   └── semantic_web_jena_clj
│       └── core.clj
├── src-java
│   └── main
│       ├── java
│       │   └── com
│       │       └── markwatson
│       │           └── semanticweb
│       │               ├── Cache.java
│       │               ├── JenaApis.java
│       │               └── QueryResult.java
│       └── resources
│           └── log4j.xml
└── test
    └── semantic_web_jena_clj
        └── core_test.clj
~~~~~~~~

I don't list the file **JenaApis.java** here but you might want to have it open in an editor while reading the following listing of the Clojure code that wraps this Java code:
        
{lang="clojure",linenos=on}
~~~~~~~~
(ns semantic-web-jena-clj.core
  (:import (com.markwatson.semanticweb JenaApis Cache
                                       QueryResult)))

(defn- get-jena-api-model
  "get a default model with OWL reasoning"
  []
  (new JenaApis))

(defonce model (get-jena-api-model))

(defn- results->clj [results]
  (let [variable-list (seq (. results variableList))
        bindings-list (seq (map seq (. results rows)))]
    (cons variable-list bindings-list)))

(defn load-rdf-file [fpath]
  (. model loadRdfFile fpath))

(defn query "SPARQL query" [sparql-query]
  (results->clj (. model query sparql-query)))

(defn query-remote
 "remote service like DBPedia, etc."
 [remote-service sparql-query]
  (results->clj
    (. model queryRemote remote-service sparql-query)))

(defn query-dbpedia [sparql-query]
  (query-remote "https://dbpedia.org/sparql" sparql-query))

(defn query-wikidata [sparql-query]
  (query-remote
    "https://query.wikidata.org/bigdata/namespace/wdq/sparql" sparql-query))
~~~~~~~~


Test code:

{lang="clojure",linenos=on}
~~~~~~~~
(ns semantic-web-jena-clj.core-test
  (:require [clojure.test :refer :all]
            [semantic-web-jena-clj.core :refer :all]))

(deftest load-data-and-sample-queries
  (testing
    "Load local triples files and SPARQL queries"
    (load-rdf-file "data/sample_news.nt")
    (let [results (query "select * { ?s ?p ?o } limit 5")]
      (println results))
    (is (= 0 0))))

(deftest dbpedia-test
  (testing "Try SPARQL query to DBPedia endpoint"
    (println
      (query-dbpedia
        "select ?p where { <http://dbpedia.org/resource/Bill_Gates> ?p <http://dbpedia.org/resource/Microsoft> . } limit 10"))))

(deftest wikidata-test
  (testing "Try SPARQL query to WikiData endpoint"
    (println
      (query-dbpedia
        "select * where { ?subject ?property ?object . } limit 10"))))
~~~~~~~~

Output will look like (reformatted for readability and most output is not shown):

{linenos=on}
~~~~~~~~
((subject property object)
 (http://www.openlinksw.com/virtrdf-data-formats#default-iid
  http://www.w3.org/1999/02/22-rdf-syntax-ns#type
  http://www.openlinksw.com/schemas/virtrdf#QuadMapFormat)
 (http://www.openlinksw.com/virtrdf-data-formats#default-iid-nullable
  http://www.w3.org/1999/02/22-rdf-syntax-ns#type
  http://www.openlinksw.com/schemas/virtrdf#QuadMapFormat)
  ...
~~~~~~~~

Data consists of nested lists where the first sub-list is the SPARQL query variable names, in this case: **subject property object**. Subsequent sub-lists are binding values for the query variables.

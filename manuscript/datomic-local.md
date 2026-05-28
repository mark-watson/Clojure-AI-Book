# Datomic Local: Clojure wrapper for Immutable Datalog

In the previous chapters we used vector databases for storing embeddings and we used the Apache Jena library for working with RDF triples and SPARQL queries. In this chapter we'll explore another approach to building knowledge graphs: Datomic Local, a free embedded Datalog database from Cognitect.

Datomic is an **immutable database**. Data is never updated or deleted in place — each transaction creates new *datoms* (facts) that are appended to the transaction log. This append-only model has several properties that make it a natural fit for AI applications:

- **Time travel:** Query the database as it existed at any past transaction
- **Audit trails:** See every assertion and retraction across time
- **Reproducibility:** Queries against a specific database snapshot always return the same results
- **Fact accumulation:** Add facts incrementally without losing history — ideal for agent memory and knowledge bases

Datomic Local runs **in-process** with no separate server or transactor, making it ideal for prototyping knowledge-base and AI applications. It uses the Datomic client API (`datomic.client.api`) and stores data on the local filesystem or in memory. It is free (Apache 2.0 license) and requires no license key.

## Why Datalog for AI?

Datomic's query language is Datalog, a declarative logic programming language. Unlike SQL, Datalog queries are built from patterns of *clauses* that the query engine unifies against the database. This pattern-matching style is a natural fit for querying knowledge graphs:

- **Entity-attribute-value model:** Datomic's data model is EAV — every fact is `[entity attribute value]` — which maps directly to RDF's subject-predicate-object triples
- **Reference types:** Attributes can be typed as `:db.type/ref`, creating graph edges between entities
- **Joins are implicit:** In Datalog, joining across entities is done by reusing the same variable in multiple clauses, not by explicit JOIN syntax
- **Pull API:** Navigate entity relationships declaratively, following refs to arbitrary depth
- **Rules:** Datalog supports user-defined rules for recursive queries and inference

Let's jump into the code.

## Project Setup

The example project is in `source-code/datomic_local`. Dependencies are minimal — just Clojure and the Datomic Local library:

```clojure
;; deps.edn
{:paths ["src" "test"]
 :deps {org.clojure/clojure {:mvn/version "1.11.1"}
        com.datomic/local {:mvn/version "1.0.291"}}}

;; project.clj
(defproject datomic-local "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.datomic/local "1.0.291"]])
```

You can use either Leiningen (recommended for this project) or the Clojure CLI:

```
lein test      # run all tests
lein repl      # start a REPL

clj -M:test    # Clojure CLI
clj            # start a REPL
```

## A Thin Wrapper

The file `src/datomic_local/core.clj` provides a thin wrapper around `datomic.client.api`. The wrapper reduces boilerplate by handling the arg-map wrapping that the underlying API requires, but it doesn't hide the Datomic concepts — you can use `datomic.client.api` directly if you prefer. The wrapper is purely for convenience.

Here's the namespace declaration and the client creation functions:

```clojure
(ns datomic-local.core
  "Thin wrapper around Datomic Local (com.datomic/local), the free embedded
   Datalog database."
  (:require [datomic.client.api :as d]))

(defn client
  "Create a Datomic Local client."
  ([arg]
   (if (map? arg)
     (d/client arg)
     (d/client {:server-type :datomic-local :system arg})))
  ([system & {:keys [storage-dir]}]
   (let [config (cond-> {:server-type :datomic-local :system system}
                  storage-dir (assoc :storage-dir storage-dir))]
     (d/client config))))
```

The `client` function is flexible — you can pass a full config map, or just a system name string:

```clojure
;; Full config (in-memory, ideal for tests and REPL experimentation)
(def client (d/client {:server-type :dev-local
                       :storage-dir :mem
                       :system "my-system"}))

;; Short form (persistent storage under ~/.datomic/)
(def client (d/client "my-system"))

;; Short form with custom storage directory
(def client (d/client "my-system" :storage-dir "/data/datomic"))
```

For persistent storage, data is stored on disk under `~/.datomic/${system}/${db-name}/` and survives JVM restarts. For development and testing, `:storage-dir :mem` keeps everything in memory.

## Database Lifecycle

Once you have a client, you create databases, connect to them, and manage the lifecycle:

```clojure
(defn create-database [client db-name]
  (d/create-database client {:db-name db-name}))

(defn connect [client db-name]
  (d/connect client {:db-name db-name}))

(defn delete-database [client db-name]
  (d/delete-database client {:db-name db-name}))

(defn list-databases [client]
  (d/list-databases client {}))
```

There's also a convenience function `create-db` that combines client creation, database creation, and connection in one call:

```clojure
(defn create-db
  ([db-name] (create-db "default" db-name))
  ([system db-name]
   (let [c (client system)]
     (d/create-database c {:db-name db-name})
     (d/connect c {:db-name db-name}))))
```

## Transactions

All writes to Datomic happen through transactions. A transaction is a vector of entity maps or operation vectors that is committed atomically:

```clojure
(defn transact [conn tx-data]
  (d/transact conn {:tx-data tx-data}))
```

The transaction returns a result map synchronously:

```clojure
{:db-before <db-value>   ; database snapshot before the transaction
 :db-after  <db-value>   ; database snapshot after the transaction
 :tx-data   [<datoms>]   ; datoms produced by the transaction
 :tempids   {<str> <id>} ; resolved temporary IDs -> real entity IDs}
```

One of Datomic's most useful features is **string tempids**. When you need to create entities that reference each other within the same transaction, use `{:db/id "some-string"}` as a placeholder:

```clojure
(let [tx (d/transact conn
           [{:db/id "alice" :person/name "Alice"}
            {:db/id "bob"   :person/name "Bob" :person/friend ["alice"]}])]
  (println "Alice's entity ID:" (get (:tempids tx) "alice"))
  (println "Bob's entity ID:"   (get (:tempids tx) "bob")))
```

After the transaction commits, the `:tempids` map gives you the real numeric entity IDs.

## Schema Definition

In Datomic, every attribute you use must be defined in the schema before you can assert values for it. Each attribute definition specifies the attribute's identity, value type, cardinality, and optional properties:

```clojure
(defn define-schema [conn schema]
  (d/transact conn {:tx-data schema}))
```

The `define-schema` function is just `transact` with a more descriptive name — attribute definitions are themselves just entity data.

Here's the movie schema we use throughout the examples:

```clojure
(def movie-schema
  [{:db/ident       :movie/title
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Movie title (unique)"}

   {:db/ident       :movie/year
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Release year"}

   {:db/ident       :movie/genre
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Genre"}

   {:db/ident       :movie/director
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "Director -- ref to :person entity"}

   {:db/ident       :movie/cast
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc         "Cast members -- refs to :person entities"}

   {:db/ident       :person/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Person's name"}])
```

Key points about schema design:

- **`:db.unique/identity`** — attempting to assert the same value for a different entity triggers an **upsert**: Datomic resolves the existing entity and merges the new attributes into it. Best for natural keys like titles, emails, usernames.
- **`:db.unique/value`** — attempting to assert the same value throws an error. Best for enforcing uniqueness without upsert semantics.
- **`:db.cardinality/one`** — each entity has at most one value. Asserting a new value retracts the old one.
- **`:db.cardinality/many`** — each entity can have multiple values. Asserting a new value adds to the set.
- **`:db.type/ref`** — creates a reference to another entity, forming graph edges. Use `:db.cardinality/many` with `:db.type/ref` for to-many relationships (like a movie's cast).
- **`:db/index true`** — adds an AVET index for faster lookups on attributes you'll frequently query.

## Queries

The query function is the heart of working with Datomic:

```clojure
(defn q [query & args]
  (apply d/q query args))
```

Datalog queries are EDN data structures with `:find`, optional `:in`, and `:where` clauses. Let's build up from simple queries to more complex ones.

### Basic Queries

After inserting some movie entities, we can find all titles:

```clojure
(d/transact conn
  [{:movie/title "The Matrix"   :movie/year 1999 :movie/genre "Sci-Fi"}
   {:movie/title "Inception"    :movie/year 2010 :movie/genre "Sci-Fi"}
   {:movie/title "The Godfather" :movie/year 1972 :movie/genre "Crime"}
   {:movie/title "Pulp Fiction" :movie/year 1994 :movie/genre "Crime"}])

(let [db (d/db conn)]
  (d/q '[:find ?title
         :where [?e :movie/title ?title]]
       db))
;; => #{["Pulp Fiction"] ["The Godfather"] ["The Matrix"] ["Inception"]}
```

The `:where` clause `[?e :movie/title ?title]` is a pattern that matches any entity `?e` that has a `:movie/title` attribute with value `?title`. The query engine finds all satisfying combinations of variables.

Multiple clauses in a `:where` act as implicit AND:

```clojure
;; Find Sci-Fi movies with their release years
(d/q '[:find ?title ?year
       :where [?e :movie/title ?title]
              [?e :movie/year ?year]
              [?e :movie/genre "Sci-Fi"]]
     db)
;; => #{["The Matrix" 1999] ["Inception" 2010]}
```

Notice how `?e` appears in all three clauses — this is how Datalog expresses joins. The same variable in multiple patterns constrains them to refer to the same entity.

### Predicate Functions

You can use Clojure functions in Datalog queries to filter results:

```clojure
;; Movies released after 2000
(d/q '[:find ?title
       :where [?e :movie/title ?title]
              [?e :movie/year ?year]
              [(> ?year 2000)]]
     db)
;; => #{["Inception"]}
```

The expression `[(> ?year 2000)]` calls the Clojure `>` function with the bound value of `?year`. Any Clojure function can be used as a predicate — the clause succeeds when the function returns truthy.

### Parameterized Queries

The `:in` clause lets you parameterize queries, making them reusable across different inputs:

```clojure
;; Scalar parameter: find movies from a specific year
(d/q '[:find ?title
       :in $ ?year
       :where [?e :movie/year ?year]
              [?e :movie/title ?title]]
     db 1999)
;; => #{["The Matrix"]}

;; Collection parameter: find movies matching any of several genres
(d/q '[:find ?title
       :in $ [?genre ...]
       :where [?e :movie/genre ?genre]
              [?e :movie/title ?title]]
     db ["Sci-Fi" "Crime"])
;; => #{["The Matrix"] ["Inception"] ["The Godfather"] ["Pulp Fiction"]}

;; Tuple parameter: find movies matching year-genre pairs
(d/q '[:find ?title
       :in $ [[?year ?genre]]
       :where [?e :movie/year ?year]
              [?e :movie/genre ?genre]
              [?e :movie/title ?title]]
     db [[1999 "Sci-Fi"] [1972 "Crime"]])
;; => #{["The Matrix"] ["The Godfather"]}
```

The `$` in `:in` refers to the database. Additional parameters after the database value correspond to additional `:in` bindings.

### Query Result Shapes

Datomic Local's client API only supports **find-rel** (tuple return). Here's how different query shapes map to results:

| Query `:find` | Returns | Extract with |
|---------------|---------|--------------|
| `:find ?a` | `#{["val1"] ["val2"]}` | `(set (map first result))` |
| `:find ?a ?b` | `#{["a1" "b1"] ["a2" "b2"]}` | Direct set containment |
| `:find (count ?e)` | `#{[4]}` | `(ffirst result)` |
| `:find ?a (count ?e)` | `#{["Sci-Fi" 2] ["Crime" 2]}` | Direct set access |
| `:find (pull ?e [:*])` | `#{[{...}]}` | `(ffirst result)` |

The peer-library syntax for scalar return (`:find ?a .`) and collection binding (`:find [?a ...]`) is not available in the client API, but `ffirst` and `(map first result)` handle the same needs.

### Aggregates

Datalog supports aggregation functions including `count`, `min`, `max`, `sum`, and `avg`:

```clojure
;; Total movie count
(ffirst (d/q '[:find (count ?e)
               :where [?e :movie/title]]
             db))
;; => 4

;; Count per genre
(d/q '[:find ?genre (count ?e)
       :where [?e :movie/genre ?genre]]
     db)
;; => #{["Sci-Fi" 2] ["Crime" 2]}

;; Earliest and latest release years
(ffirst (d/q '[:find (min ?year)
               :where [?e :movie/year ?year]]
             db))
;; => 1972

(ffirst (d/q '[:find (max ?year)
               :where [?e :movie/year ?year]]
             db))
;; => 2010
```

## Entity Relationships and Nested Pull

The Pull API lets you navigate entity relationships declaratively:

```clojure
(defn pull [db selector eid]
  (d/pull db selector eid))
```

Here's a complete example that creates people and movies with cross-references, then pulls nested data:

```clojure
;; Create entities with string tempids for cross-referencing
(let [tx (d/transact conn
           [{:db/id "wachowski"  :person/name "Lana Wachowski"}
            {:db/id "nolan"      :person/name "Christopher Nolan"}
            {:db/id "reeves"     :person/name "Keanu Reeves"}
            {:db/id "dicaprio"   :person/name "Leonardo DiCaprio"}

            {:movie/title    "The Matrix"
             :movie/year     1999
             :movie/genre    "Sci-Fi"
             :movie/director "wachowski"
             :movie/cast     ["reeves"]}

            {:movie/title    "Inception"
             :movie/year     2010
             :movie/genre    "Sci-Fi"
             :movie/director "nolan"
             :movie/cast     ["dicaprio"]}])
      tempids (:tempids tx)
      db (d/db conn)]

  ;; Find entity ID for The Matrix
  (let [matrix-eid (ffirst (d/q '[:find ?e
                                   :where [?e :movie/title "The Matrix"]]
                                 db))]

    ;; Pull nested data: movie + director + cast
    (d/pull db
            [:movie/title :movie/year
             {:movie/director [:person/name]}
             {:movie/cast [:person/name]}]
            matrix-eid)))
;; => {:movie/title "The Matrix",
;;     :movie/year 1999,
;;     :movie/director {:person/name "Lana Wachowski"},
;;     :movie/cast [{:person/name "Keanu Reeves"}]}
```

The pull selector `[:movie/title :movie/year {:movie/director [:person/name]} {:movie/cast [:person/name]}]` says: give me the title and year, and for the director and cast refs, follow them and pull the person's name. This declarative approach to entity navigation is one of Datomic's most powerful features.

Pull selectors support a rich set of patterns:

| Selector | Description |
|----------|-------------|
| `'[*]` | All attributes (refs show as `{:db/id N}`) |
| `:attr-name` | Single attribute value |
| `[:attr1 :attr2]` | Multiple attributes |
| `{:attr [:sub-attr]}` | Follow a ref and pull sub-attributes |
| `{:attr [:*]}` | Follow a ref and pull all sub-attributes |
| `'[(:attr :default)]` | Default value if attribute is missing |
| `'[(:attr :as :alias)]` | Rename attribute in result |

You can also use pull expressions inside queries:

```clojure
(d/q '[:find (pull ?e [:movie/title :movie/year
                        {:movie/director [:person/name]}])
       :where [?e :movie/title]]
     db)
;; => #{[{:movie/title "Inception",
;;        :movie/year 2010,
;;        :movie/director {:person/name "Christopher Nolan"}}]}
```

## Join Queries

Because Datalog uses variables for implicit joins, queries that traverse relationships are concise. To find all movies by Christopher Nolan:

```clojure
(d/q '[:find ?title
       :where [?p :person/name "Christopher Nolan"]
              [?m :movie/director ?p]
              [?m :movie/title ?title]]
     db)
;; => #{["Inception"]}
```

The variable `?p` is bound to the entity representing Christopher Nolan (via `:person/name`), then `?m` is constrained to be an entity whose `:movie/director` is `?p`, and finally we extract that movie's title. This is a two-hop join expressed in three clauses, with no explicit JOIN syntax.

To find all actor-movie pairs:

```clojure
(d/q '[:find ?actor-name ?movie-title
       :where [?p :person/name ?actor-name]
              [?m :movie/cast ?p]
              [?m :movie/title ?movie-title]]
     db)
;; => #{["Keanu Reeves" "The Matrix"] ["Leonardo DiCaprio" "Inception"]}
```

## Upsert with Unique Identity

When an attribute has `:db/unique/identity`, asserting an entity with a matching value resolves the existing entity rather than creating a new one. This is called upsert:

```clojure
;; First insert
(d/transact conn
  [{:movie/title "The Matrix" :movie/year 1999 :movie/genre "Sci-Fi"}])

;; Upsert: same title resolves existing entity, genre is updated
(d/transact conn
  [{:movie/title "The Matrix" :movie/genre "Action"}])

;; Verify: one entity, genre is now "Action"
(let [db (d/db conn)
      eid (ffirst (d/q '[:find ?e
                          :where [?e :movie/title "The Matrix"]]
                        db))]
  (:movie/genre (d/pull db '[:movie/genre] eid)))
;; => "Action"
```

Because `:movie/genre` has `:db.cardinality/one`, asserting a new value retracts the old one. If it had `:db.cardinality/many`, the new value would be added to the set instead.

## Transaction Operations

Beyond entity maps, transactions support direct operations:

```clojure
;; Add an attribute value to an existing entity
[:db/add entity-id :attribute value]

;; Retract an attribute value
[:db/retract entity-id :attribute value]

;; Retract an entire entity
[:db/retractEntity entity-id]

;; Compare-and-swap (only transact if attribute has expected value)
[:db/cas entity-id :attribute old-value new-value]
```

Here's an example that retracts an existing value and adds a new one:

```clojure
(let [db (d/db conn)
      eid (ffirst (d/q '[:find ?e
                          :where [?e :movie/title "Eraserhead"]]
                        db))]
  ;; Retract the original genre
  (d/transact conn [[:db/retract eid :movie/genre "Surrealist"]])
  ;; Add a new genre
  (d/transact conn [[:db/add eid :movie/genre "Experimental"]]))
```

## Time Travel: Querying Past States

Datomic's immutability means every transaction is preserved. You can query the database as it existed at any point in the past:

```clojure
(defn as-of [db time-point]
  (d/as-of db time-point))

(defn since [db time-point]
  (d/since db time-point))

(defn history [db]
  (d/history db))
```

Here's a time travel example:

```clojure
;; Day 1: add Jaws
(let [tx1 (d/transact conn
            [{:movie/title "Jaws" :movie/year 1975 :movie/genre "Thriller"}])]
  
  ;; Day 2: add Star Wars
  (d/transact conn
    [{:movie/title "Star Wars" :movie/year 1977 :movie/genre "Sci-Fi"}])
  
  ;; Current state: both movies exist
  (ffirst (d/q '[:find (count ?e)
                 :where [?e :movie/title]]
               (d/db conn)))
  ;; => 2
  
  ;; As-of Day 1: only Jaws exists
  (let [past-db (d/as-of (d/db conn) (:t (:db-after tx1)))]
    (ffirst (d/q '[:find (count ?e)
                   :where [?e :movie/title]]
                 past-db)))
  ;; => 1
  
  (let [past-db (d/as-of (d/db conn) (:t (:db-after tx1)))]
    (d/q '[:find ?title
           :where [?e :movie/title ?title]]
         past-db))
  ;; => #{["Jaws"]}
```

This capability is particularly valuable for AI systems that need to reason about what was known at a specific time, or that need to maintain an audit trail of all fact assertions and retractions.

There are two other temporal functions worth knowing about:

- **`since`** — returns a database with only datoms added after a given time point. Useful for seeing what changed between two transactions.
- **`history`** — returns a database containing all assertions and retractions across time. Pass this to `q`, `datoms`, or `index-range` to see the full history of every fact.

## Incremental Fact Building

The append-only model encourages building knowledge bases incrementally. Facts accumulate over time, and queries across all facts always include historical data:

```clojure
;; Day 1: add two Matrix films
(d/transact conn
  [{:movie/title "The Matrix" :movie/year 1999 :movie/genre "Sci-Fi"}
   {:movie/title "The Matrix Reloaded" :movie/year 2003 :movie/genre "Sci-Fi"}])

;; Day 2: add the third Matrix film
(d/transact conn
  [{:movie/title "The Matrix Revolutions" :movie/year 2003 :movie/genre "Sci-Fi"}])

;; Query across all facts
(d/q '[:find ?title
       :where [?e :movie/genre "Sci-Fi"]
              [?e :movie/title ?title]]
     (d/db conn))
;; => #{["The Matrix"] ["The Matrix Reloaded"] ["The Matrix Revolutions"]}
```

## Differences from Datomic Pro/Cloud

Datomic Local is free and embedded, but it has some differences from the full Datomic products:

| Feature | Datomic Local | Datomic Pro/Cloud |
|---------|---------------|-------------------|
| Architecture | Embedded library | Client-server |
| Storage | Local disk or memory | Distributed storage |
| Query engine | In-process | Remote query groups |
| Scalability | Single process | Horizontally scalable |
| Scalar return (`.`) | Not supported | Supported |
| Collection binding (`[...]`) | Not supported | Supported |
| License | Apache 2.0 | Commercial |
| Cost | Free | Paid |

## A Helper Function

The wrapper includes one small helper that turns a common pattern into a single call:

```clojure
(defn find-entity [db attr val]
  (ffirst (d/q '[:find ?e :in $ ?a ?v :where [?e ?a ?v]] db attr val)))

;; Usage:
(find-entity db :movie/title "The Matrix")
;; => 96757023244367
```

## AI & Knowledge Graph Use Cases

Datomic Local is particularly well-suited for several AI application patterns:

- **Knowledge graphs:** The entity-attribute-ref model maps naturally to RDF-style graphs. Define schemas for your domain entities, create them with tempids, and use pull expressions to navigate relationships.
- **Fact accumulation:** The append-only model means you can add facts incrementally from NLP pipelines, web scraping, or agent observations without losing history or dealing with update conflicts.
- **Temporal reasoning:** `as-of` and `since` enable querying what was known at any point in time — essential for building AI systems that need to reason about the evolution of knowledge.
- **Explainability:** Every fact assertion and retraction is preserved. You can trace exactly when and in what transaction any piece of knowledge entered the system.
- **Agent memory:** The combination of string tempids for cross-referencing, nested pull for context retrieval, and time travel for episodic memory makes Datomic an excellent substrate for AI agent state.
- **Hybrid retrieval:** Combine Datalog queries with full-text indexing (`:db/fulltext true`) for applications that need both structured and unstructured search.

## Running the Tests

The test file `test/datomic_local/core_test.clj` contains 10 tests with 61 assertions covering the full API surface. The tests are designed to be read from top to bottom as a tutorial:

```bash
lein test      # run all tests
lein test :only datomic-local.core-test/relationships-test  # run a single test
lein repl      # explore interactively
```

The tests cover: database lifecycle, basic entities and queries, entity relationships, parameterized queries, the pull API, upsert behavior, aggregates, transaction operations, time travel, and incremental fact building.

## Resources

- [Datomic Local Documentation](https://docs.datomic.com/datomic-local.html)
- [Datomic Client API Reference](https://docs.datomic.com/reference/client-reference.html)
- [Datalog Query Reference](https://docs.datomic.com/cloud/query/query-data-reference.html)
- [Pull API Reference](https://docs.datomic.com/cloud/query/query-pull.html)
- [Transaction Reference](https://docs.datomic.com/cloud/transactions/transaction-data-reference.html)

In the next chapter we'll continue building practical AI tools with Clojure, applying the knowledge graph techniques we've covered across the Jena, SPARQL, and Datomic chapters.

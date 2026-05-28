# Datomic Local — Example for "Practical Artificial Intelligence Programming With Clojure"

This directory contains a Clojure wrapper and comprehensive tests for
[Datomic Local](https://docs.datomic.com/datomic-local.html)
(`com.datomic/local`), the free embedded Datalog database from Cognitect.

Datomic Local runs **in-process** with no separate server or transactor, making
it ideal for prototyping knowledge-base and AI applications. It uses the
Datomic client API (`datomic.client.api`) and stores data on the local
filesystem or in memory.

## Quick Start

```clojure
(require '[datomic-local.core :as d])

;; Create an in-memory client (no disk, ideal for tests/REPL)
(def client (d/client {:server-type :dev-local
                       :storage-dir :mem
                       :system "my-system"}))

;; Create a database and connect
(def db-name "my-kb")
(d/create-database client db-name)
(def conn (d/connect client db-name))

;; Define a schema
(d/transact conn
  [{:db/ident       :person/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :person/friend
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])

;; Add entities — use string tempids for cross-references
(d/transact conn
  [{:db/id "alice" :person/name "Alice"}
   {:db/id "bob"   :person/name "Bob" :person/friend ["alice"]}])

;; Query with Datalog
(d/q '[:find ?name
       :where [?e :person/name ?name]]
     (d/db conn))
;; => #{["Alice"] ["Bob"]}

;; Pull nested entity data
(let [db (d/db conn)
      eid (ffirst (d/q '[:find ?e
                         :where [?e :person/name "Bob"]]
                       db))]
  (d/pull db [:person/name {:person/friend [:person/name]}] eid))
;; => {:person/name "Bob", :person/friend [{:person/name "Alice"}]}
```

## Project Structure

```
datomic_local/
  deps.edn          — Clojure CLI deps (clj -M:test)
  project.clj        — Leiningen deps (lein test)
  README.md          — This file
  src/
    datomic_local/
      core.clj        — Wrapper package
  test/
    datomic_local/
      core_test.clj   — Tests that serve as examples
```

## Dependencies

- **Clojure** 1.11.1+
- **com.datomic/local** 1.0.291 — the free embedded Datomic library
- No server, no transactor, no license key required

## Build Tools

**Leiningen** (recommended for this project):
```
lein test      # run all tests
lein repl      # start a REPL
```

**Clojure CLI**:
```
clj -M:test    # run all tests (after adding test runner alias)
clj            # start a REPL
```

## Wrapper API Reference

The wrapper (`datomic-local.core`) is a thin layer over `datomic.client.api` that
reduces boilerplate by hiding arg-map wrapping. You can also use
`datomic.client.api` directly — the wrapper is purely for convenience.

### Client & Database Lifecycle

#### `(client arg)`
Create a Datomic Local client. Accepts either a config map or a system name
string.

```clojure
;; Full config map (for in-memory or custom storage)
(def client (d/client {:server-type :dev-local
                       :storage-dir :mem
                       :system "my-system"}))

;; Short form: just the system name (uses default storage at ~/.datomic/)
(def client (d/client "my-system"))

;; Short form with storage override
(def client (d/client "my-system" :storage-dir "/custom/path"))
```

**Config map keys:**

| Key | Required | Description |
|-----|----------|-------------|
| `:server-type` | Yes | `:dev-local` or `:datomic-local` (both work) |
| `:system` | Yes | System name — databases are scoped per system |
| `:storage-dir` | No | Override storage dir. Use `:mem` for in-memory (no persistence) |

#### `(create-database client db-name)`
Create a new database. `db-name` is a plain string (not a map).

```clojure
(d/create-database client "my-db")
;; => true
```

#### `(connect client db-name)`
Connect to an existing database. Returns a connection.

```clojure
(def conn (d/connect client "my-db"))
```

#### `(create-db system db-name)` / `(create-db db-name)`
Convenience: create client, database, and connect in one call.

```clojure
;; Full form
(def conn (d/create-db "my-system" "my-db"))

;; Short form (system defaults to "default")
(def conn (d/create-db "my-db"))
```

#### `(delete-database client db-name)`
Delete a database and its storage.

```clojure
(d/delete-database client "my-db")
```

#### `(list-databases client)`
List all databases in the system.

```clojure
(d/list-databases client)
;; => ["my-db" "other-db"]
```

### Database Values

#### `(db conn)`
Get the current database value (an immutable point-in-time snapshot).
All queries run against a `db` value, not a connection.

```clojure
(def current-db (d/db conn))
```

#### `(as-of db time-point)`
Return a database value as of a past transaction. Enables time-travel queries.

```clojure
(let [tx (d/transact conn [{:person/name "Alice"}])]
  ;; Later, look back to the state after that transaction:
  (def past-db (d/as-of (d/db conn) (:t (:db-after tx))))
  (d/q '[:find ?n :where [?e :person/name ?n]] past-db))
```

#### `(since db time-point)`
Return a database value with only datoms added since the given time-point.

```clojure
(d/since (d/db conn) some-t)
```

#### `(history db)`
Return a database containing all assertions and retractions across time.
Pass to `q`, `datoms`, or `index-range` to see the full history.

```clojure
(d/history (d/db conn))
```

### Transactions

#### `(transact conn tx-data)`
Transact data synchronously. `tx-data` is a vector of entity maps or
transaction operations. Returns a result map.

```clojure
;; Entity maps
(d/transact conn
  [{:person/name "Alice"}
   {:person/name "Bob"}])

;; Direct operations
(d/transact conn
  [[:db/add eid :person/name "Charlie"]
   [:db/retract eid :person/name "Bob"]])
```

**Transaction result map:**

```clojure
{:db-before <db-value>   ; database before the transaction
 :db-after  <db-value>   ; database after the transaction
 :tx-data   [<datoms>]   ; datoms produced by the transaction
 :tempids   {<str> <id>} ; resolved tempid -> entity ID mappings}
```

#### String TempIDs
Use `{:db/id "some-string"}` to create temporary IDs that can be referenced
within the same transaction. After commit, real numeric entity IDs are
assigned — look them up in `(:tempids result)`.

```clojure
(let [tx (d/transact conn
           [{:db/id "alice" :person/name "Alice"}
            {:db/id "bob"   :person/name "Bob" :person/friend ["alice"]}])]
  (println "Alice's entity ID:" (get (:tempids tx) "alice"))
  (println "Bob's entity ID:"   (get (:tempids tx) "bob")))
```

### Queries

#### `(q query & inputs)`
Execute a Datalog query. The query is an EDN vector with `:find`, optional
`:in`, and `:where` clauses. Additional args after the database become `:in`
parameters.

**Important:** The client API only supports *find-rel* (tuple returns).
The `.` (scalar) and `[...]` (collection binding) syntax from the peer
library are not available. Use `ffirst` to extract scalar aggregates and
`(set (map first ...))` for collections.

```clojure
;; Basic query
(d/q '[:find ?title ?year
       :where [?e :movie/title ?title]
              [?e :movie/year ?year]]
     db)
;; => #{["The Matrix" 1999] ["Inception" 2010]}

;; With scalar parameter
(d/q '[:find ?title
       :in $ ?year
       :where [?e :movie/year ?year]
              [?e :movie/title ?title]]
     db 1999)
;; => #{["The Matrix"]}

;; With collection parameter
(d/q '[:find ?title
       :in $ [?genre ...]
       :where [?e :movie/genre ?genre]
              [?e :movie/title ?title]]
     db ["Sci-Fi" "Crime"])
;; => #{["The Matrix"] ["Inception"] ["The Godfather"] ["Goodfellas"]}

;; With tuple parameter
(d/q '[:find ?title
       :in $ [[?year ?genre]]
       :where [?e :movie/year ?year]
              [?e :movie/genre ?genre]
              [?e :movie/title ?title]]
     db [[1999 "Sci-Fi"] [1972 "Crime"]])
;; => #{["The Matrix"] ["The Godfather"]}

;; Aggregates (return set of single-element vectors — use ffirst to extract)
(ffirst (d/q '[:find (count ?e)
               :where [?e :movie/title]]
             db))
;; => 4

;; Grouped aggregates
(d/q '[:find ?genre (count ?e)
       :where [?e :movie/genre ?genre]]
     db)
;; => #{["Sci-Fi" 2] ["Crime" 2]}

;; Predicate functions (Clojure fns in query)
(d/q '[:find ?title
       :where [?e :movie/title ?title]
              [?e :movie/year ?year]
              [(> ?year 2000)]]
     db)
;; => #{["Inception"]}

;; Pull inside query
(d/q '[:find (pull ?e [:movie/title {:movie/director [:person/name]}])
       :where [?e :movie/title]]
     db)
;; => #{[{:movie/title "Inception",
;;        :movie/director {:person/name "Christopher Nolan"}}]}
```

### Schema

#### `(define-schema conn schema)`
Define a schema by transacting a vector of attribute definitions. This is just
a convenience wrapper around `(transact conn schema)`.

```clojure
(d/define-schema conn
  [{:db/ident       :movie/title
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Movie title (unique)"}

   {:db/ident       :movie/tags
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc         "Tags (many per movie)"}

   {:db/ident       :movie/director
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "Director ref"}])
```

**Attribute definition reference:**

| Key | Required | Values |
|-----|----------|--------|
| `:db/ident` | Yes | Keyword ident, e.g. `:movie/title` |
| `:db/valueType` | Yes | `:db.type/string`, `:db.type/long`, `:db.type/ref`, `:db.type/boolean`, `:db.type/instant`, `:db.type/float`, `:db.type/double`, `:db.type/bigdec`, `:db.type/bigint`, `:db.type/keyword`, `:db.type/uuid`, `:db.type/bytes` |
| `:db/cardinality` | Yes | `:db.cardinality/one` or `:db.cardinality/many` |
| `:db/unique` | No | `:db.unique/identity` (upsert by this value) or `:db.unique/value` (unique constraint) |
| `:db/doc` | No | Documentation string |
| `:db/index` | No | `true` — add AVET index for faster lookups |
| `:db/fulltext` | No | `true` — enable full-text search |
| `:db/isComponent` | No | `true` — ref'd entity is owned (cascading retraction) |
| `:db/noHistory` | No | `true` — don't retain history for this attribute |

### Pull API

#### `(pull db selector eid)`
Pull entity data using a declarative selector pattern.

```clojure
;; All attributes
(d/pull db '[*] eid)
;; => {:movie/title "Inception", :movie/year 2010, :movie/director {:db/id 123}, ...}

;; Specific attributes
(d/pull db [:movie/title :movie/year] eid)
;; => {:movie/title "Inception", :movie/year 2010}

;; Nested expansion (follow refs)
(d/pull db [:movie/title
            {:movie/director [:person/name]}
            {:movie/cast [:person/name]}]
        eid)
;; => {:movie/title "Inception",
;;     :movie/director {:person/name "Christopher Nolan"},
;;     :movie/cast [{:person/name "Leonardo DiCaprio"}]}

;; Pull inside a query
(d/q '[:find (pull ?e [:movie/title :movie/year])
       :where [?e :movie/genre "Sci-Fi"]]
     db)
;; => #{[{:movie/title "The Matrix", :movie/year 1999}
;;       {:movie/title "Inception", :movie/year 2010}]}
```

**Pull selector reference:**

| Selector | Description |
|----------|-------------|
| `'[*]` | All attributes (refs show as `{:db/id N}`) |
| `:attr-name` | Single attribute value |
| `[:attr1 :attr2]` | Multiple attributes |
| `{:attr [:sub-attr]}` | Follow a ref and pull sub-attributes |
| `{:attr [:*]}` | Follow a ref and pull all sub-attributes |
| `'[:attr ...]` | Recursive limit (depth limit) |
| `'[(:attr :default)]` | Default value if attribute is missing |
| `'[(:attr :as :alias)]` | Rename attribute in result |

### Helpers

#### `(define-schema conn schema)`
Transact attribute definitions. Same as `(transact conn schema)` but with
documentation in the function name.

```clojure
(d/define-schema conn
  [{:db/ident :person/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])
```

#### `(find-entity db attr val)`
Find the entity ID for a unique attribute value. Returns the entity ID or nil.

```clojure
(d/find-entity db :movie/title "The Matrix")
;; => 96757023244367
```

## Transaction Operations

Beyond entity maps, transactions support direct operations:

```clojure
;; Add an attribute to an existing entity
[:db/add entity-id :attribute value]

;; Retract an attribute value from an entity
[:db/retract entity-id :attribute value]

;; Retract an entire entity (all its attributes)
[:db/retractEntity entity-id]

;; Compare-and-swap (only transact if attribute has expected value)
[:db/cas entity-id :attribute old-value new-value]
```

Example combining entity maps and operations:

```clojure
(d/transact conn
  [{:movie/title "Eraserhead" :movie/genre "Surrealist"}        ; entity map
   [:db/retract eid :movie/genre "Surrealist"]                  ; operation
   [:db/add eid :movie/genre "Experimental"]])                  ; operation
```

## Query Result Shapes

Datomic Local's client API only supports **find-rel** (tuple return). Here's
how different query shapes map to results and how to extract values:

| Query `:find` | Returns | Extract with |
|---------------|---------|--------------|
| `:find ?a` | `#{["val1"] ["val2"]}` | `(set (map first result))` |
| `:find ?a ?b` | `#{["a1" "b1"] ["a2" "b2"]}` | `(contains? (set result) ["a1" "b1"])` |
| `:find (count ?e)` | `#{[4]}` | `(ffirst result)` |
| `:find ?a (count ?e)` | `#{["Sci-Fi" 2] ["Crime" 2]}` | Direct set access |
| `:find (pull ?e [:*])` | `#{[{...}]}` | `(ffirst result)` |

**Not supported** (peer-library syntax not in client API):
- `:find ?a .` (scalar return) — use `ffirst` instead
- `:find [?a ...]` (collection binding) — use `(map first result)` instead

## Storage Options

### In-Memory (`:storage-dir :mem`)

Data lives only for the lifetime of the JVM. Best for tests, REPL
experimentation, and ephemeral knowledge bases.

```clojure
(d/client {:server-type :dev-local
           :storage-dir :mem
           :system "test-system"})
```

### Persistent (default)

Data is stored on disk under `~/.datomic/${system}/${db-name}/`.
Survives JVM restarts.

```clojure
;; Default storage
(d/client "my-system")

;; Custom storage directory
(d/client "my-system" :storage-dir "/data/datomic")
```

## Immutability & Time Model

Datomic is an **immutable database**. Data is never updated or deleted in
place — each transaction creates new *datoms* (facts) that are appended to
the transaction log. This enables:

- **Time travel:** Query the database as it existed at any past transaction
- **Audit trails:** See every assertion and retraction across time
- **Reproducibility:** Queries against a specific `db` value always return
  the same results

```clojure
;; Capture the transaction T after adding data
(def tx (d/transact conn [{:movie/title "Jaws" :movie/year 1975}]))
(def tx-t (:t (:db-after tx)))

;; Add more data
(d/transact conn [{:movie/title "Star Wars" :movie/year 1977}])

;; Now query the past — as-of tx-t, Star Wars doesn't exist yet
(def past-db (d/as-of (d/db conn) tx-t))
(d/q '[:find ?title :where [?e :movie/title ?title]] past-db)
;; => #{["Jaws"]}
```

## Schema Design Notes

### Unique Identity vs Unique Value

- **`:db.unique/identity`** — attempting to assert a different entity with the
  same value triggers an **upsert** (resolves the existing entity and adds the
  new attributes to it). Best for natural keys like titles, emails, usernames.

- **`:db.unique/value`** — attempting to assert a different entity with the
  same value throws an error. Best for enforcing uniqueness without upsert
  semantics.

### Cardinality: One vs Many

- **`:db.cardinality/one`** — each entity has at most one value for this
  attribute. Asserting a new value **retracts** the old one.

- **`:db.cardinality/many`** — each entity can have multiple values for this
  attribute. Asserting a new value **adds** to the set.

### Reference Types (`:db.type/ref`)

Use `:db.type/ref` to create relationships between entities. When pulling,
nest selectors inside maps to follow refs:

```clojure
{:movie/director [:person/name :person/birth-year]}  ; pull director attrs
```

For to-many refs (`:db.cardinality/many` + `:db.type/ref`):

```clojure
{:movie/cast [:person/name]}  ; returns vector of entity maps
```

### Indexing

Add `:db/index true` to attributes you'll frequently query on. This creates
an AVET index that speeds up lookups by attribute value.

```clojure
{:db/ident :movie/year
 :db/valueType :db.type/long
 :db/cardinality :db.cardinality/one
 :db/index true}
```

## Running the Tests

```bash
# All tests (with educational printouts)
lein test

# Run a single test
lein test :only datomic-local.core-test/relationships-test

# REPL exploration
lein repl
```

The test file (`test/datomic_local/core_test.clj`) contains 10 tests with
61 assertions covering the full API surface. Each test includes `println`
statements that show sample output — read them from top to bottom as a
tutorial.

## Test Overview

| # | Test | Concepts Demonstrated |
|---|------|-----------------------|
| 1 | `database-lifecycle-test` | Client creation, DB lifecycle, schema installation |
| 2 | `basic-entities-and-queries-test` | Entity insertion, basic Datalog, predicate filters |
| 3 | `relationships-test` | String tempids, `:db.type/ref`, join queries, nested pull |
| 4 | `parameterized-queries-test` | `:in` with scalars, collections, and tuples |
| 5 | `pull-api-test` | Pull with `'[*]`, nested expansion, pull-in-query |
| 6 | `upsert-test` | `:db.unique/identity` upsert, cardinality assertion |
| 7 | `aggregates-test` | `count`, `min`, `max` aggregates |
| 8 | `transaction-operations-test` | `:db/add`, `:db/retract` |
| 9 | `time-travel-test` | `as-of` for querying historical database states |
| 10 | `incremental-fact-building-test` | Accumulating facts, cross-temporal queries |

## AI & Knowledge Graph Use Cases

Datomic Local is well-suited for:

- **Knowledge graphs:** Entities with typed attributes and relationships
  (`:db.type/ref`) map naturally to RDF-like subject-predicate-object triples
- **Fact accumulation:** The append-only model means you can add facts
  incrementally without losing history
- **Temporal reasoning:** `as-of` and `since` enable querying what was known
  at any point in time
- **Explainability:** Full audit trail of every fact assertion and retraction
- **Agent memory:** Episodic memory for AI agents with built-in time travel
- **Semantic search:** Combine Datalog queries with full-text indexing for
  hybrid retrieval

## Differences from Datomic Pro/Cloud

Datomic Local is free and embedded, but has some differences from the full
Datomic products:

| Feature | Datomic Local | Datomic Pro/Cloud |
|---------|---------------|-------------------|
| Architecture | Embedded library | Client-server |
| Storage | Local disk or memory | Distributed storage |
| Query engine | In-process | Remote query groups |
| Scalability | Single process | Horizontally scalable |
| find-scalar (`.`) | Not supported | Supported |
| find-coll (`[...]`) | Not supported | Supported |
| License | Apache 2.0 | Commercial |
| Cost | Free | Paid |

## Resources

- [Datomic Local Documentation](https://docs.datomic.com/datomic-local.html)
- [Datomic Client API Reference](https://docs.datomic.com/reference/client-reference.html)
- [Datalog Query Reference](https://docs.datomic.com/cloud/query/query-data-reference.html)
- [Pull API Reference](https://docs.datomic.com/cloud/query/query-pull.html)
- [Transaction Reference](https://docs.datomic.com/cloud/transactions/transaction-data-reference.html)
- [Datomic Local Changelog](https://docs.datomic.com/changes/local.html)
- Book: [Practical Artificial Intelligence Programming With Clojure](https://leanpub.com/clojureai)

## Book and License

Book URI: https://leanpub.com/clojureai — read free online at
https://leanpub.com/clojureai/read

Copyright (c) 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

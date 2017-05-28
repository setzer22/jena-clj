# jena-clj

An idiomatic clojure wrapper to the Jena ontology management library. 

## Install

### Leiningen / Boot:

```clj
[jena-clj "0.1.0"]
```

### Gradle 

``` gradle
compile "jena-clj:jena-clj:0.1.0"
```

### Maven
``` xml
<dependency>
  <groupId>jena-clj</groupId>
  <artifactId>jena-clj</artifactId>
  <version>0.1.0</version>
</dependency>
```


## Usage

Some usage examples:

``` clj
(require '[jena-clj.triplestore :as ts])
(import '[org.apache.jena.query ReadWrite])
(defonce db (ts/init-database "path/to/triplestore") ; If it doesn't exist, it creates one

(with-transaction db ReadWrite/WRITE
  (ts/insert-rdf db "path/to/rdf/or/ttl/file")) ; Loads a whole RDF file into triplestore
  
(with-transaction db ReadWrite/READ
  (ts/select-query 
    "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
     SELECT ?uri ?name
     WHERE {?uri rdf:name ?name}")) ; Returns a lazy sequence with all results.
```
Take a look at triplestore.clj source for more!

### Features

- TDB Triplestore management
- SPARQL queries

## License

Copyright © 2017 setzer22

Distributed under the GNU General Public License v3.0


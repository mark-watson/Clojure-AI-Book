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
~~~~~~~~



This KGN example was hopefully both interesting to you and simple enough in its implementation (because we relied heavily on code from the last two chapters) that you feel comfortable modifying it and reusing it as a part of your own Java applications.


## Wrap-up

If you enjoyed running and experimenting with this example and want to modify it for your own projects then I hope that I provided a sufficient road map for you to do so.

I suggest further projects that you might want to try implementing with this example:

- Write a web application that processes news stories and annotates them with additional data from DBPedia and/or WikiData.
- In a web or desktop application, detect entities in text and display additional information when the user's mouse cursor hovers over a word or phrase that is identified as an entity found in DBPedia or WikiData.
- Clone this KGN example and enable it to work simultaneously with DBPedia, WikiData, and local RDF files by using three instances of the class **JenaApis** and in the main application loop access all three data sources.

I had the idea for the KGN application because I was spending quite a bit of time manually setting up SPARQL queries for DBPedia (and other public sources like WikiData) and I wanted to experiment with partially automating this process. I have experimented with versions of KGN written in Java, Hy language ([Lisp running on Python that I wrote a short book on](https://leanpub.com/hy-lisp-python/read)), Swift, and Common Lisp and all four implementations take different approaches as I experimented with different ideas. You might want to check out my [web site devoted to different versions of KGN: www.knowledgegraphnavigator.com](http://www.knowledgegraphnavigator.com/).

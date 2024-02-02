# Question Answering Using OpenAI APIs and a Local Embeddings Vector Database

The examples in this chapter are inspired by the Python LangChain and LlamaIndex projects, with just the parts I need for my projects written from scratch in Clojure. I wrote a Python book “LangChain and LlamaIndex Projects Lab Book: Hooking Large Language Models Up to the Real World Using GPT-3, ChatGPT, and Hugging Face Models in Applications” in March 2023: [https://leanpub.com/langchain](https://leanpub.com/langchain) that you might also be interested in.

The GitHub repository for this example can be found here: [https://github.com/mark-watson/Clojure-AI-Book-Code/tree/main/docs_qa](https://github.com/mark-watson/Clojure-AI-Book-Code/tree/main/docs_qa). We will be using an OpenAI API wrapper from the last chapter that you should have installed with **lein install** on your local system.

We use two models in this example: a vector embedding model
and a text completion model (see bottom of this file).
The vector embedding model is used to generate a vector embeddings for "chunks" of input documents. Here we break documents into 200 character chunks and calculate a vector embedding for each chunk. A vector dot product between two embedding vectors tells us how semantically similar two chunks of text are. We will also calculate embedding vectors for user queries and use those to find chunks that might be useful for answering the query. Useful chunks are concatenated to for a prompt for a GPT text completion model.


## Implementing a Local Vector Database for Document Embeddings

For interactive development we will read all text files in the **data** directory, create a global variable **doc-strings** containing the string contents of each file, and then create another global variable **doc-chunks** where each document string has been broken down into smaller chunks. For each chunk, we will call the OpenAI API for calculating document embeddings and store the embeddings for each chunk in the global variable **embeddings**.

When we want to query the documents in the **data** directory, we then calculate an embedding vector for the query and using a dot product calculation, efficiently find all chunks that are semantically similar to the query. The original text for these matching chunks is then combined with the user's query and passed to an OpenAI API for text completion.

For this example, we use an in-memory store of embedding vectors and chunk text. A text document is broken into smaller chunks of text. Each chunk is embedded and stored in the embeddingsStore. The chunk text is stored in the chunks array. The embeddingsStore and chunks array are used to find the most similar chunk to a prompt. The most similar chunk is used to generate a response to the prompt.



## Create Local Embeddings Vectors From Local Text Files With OpenAI GPT APIs

The code for handling OpenAI API calls is in the library **openai_api** in the GitHub repository for this book. You need to install that example project locally using:

    lien install

The code using text embeddings is located in **src/docs_qa/vectordb.clj**:

```clojure
(ns docs-qa.vectordb)

(defn string-to-floats [s]
  (map
    #(Float/parseFloat %)
    (clojure.string/split s #" ")))

(defn truncate-string [s max-length]
  (if (< (count s) max-length)
    s
    (subs s 0 max-length)))

(defn break-into-chunks [s chunk-size]
  (let [chunks (partition-all chunk-size s)]
    (map #(apply str %) chunks)))

(defn document-texts-from_dir [dir-path]
  (map
    #(slurp %)
    (rest
      (file-seq
        (clojure.java.io/file dir-path)))))

(defn document-texts-to-chunks [strings]
  (flatten
   (map #(break-into-chunks % 200) strings)))

(def directory-path "data")

(def doc-strings (document-texts-from_dir directory-path))

(def doc-chunks
  (filter
    #(> (count %) 40)
    (document-texts-to-chunks doc-strings)))

(def chunk-embeddings
  (map #(openai-api.core/embeddings %) doc-chunks))

(def embeddings-with-chunk-texts
  (map vector chunk-embeddings doc-chunks))

;;(clojure.pprint/pprint
;;  (first embeddings-with-chunk-texts))
```

If we uncomment the print statement in the last two lines of code, we see the first embedding vector and its corresponding chunk text:

```
[[-0.011284076
  -0.0110755935
  -0.011531647
  -0.0374746
  -0.0018975098
  -0.0024985236
  0.0057560513 ...
  ]
 "Amyl alcohol is an organic compound with the formula C 5 H 12 O. All eight isomers of amyl alcohol are known. The most important is isobutyl carbinol, this being the chief constituent of fermentation "]
]
```


## Using Local Embeddings Vector Database With OpenAI GPT APIs

The main application code is in the file **src/docs_qa/core.clj**:

```clojure
(ns docs-qa.core
  (:require [clojure.java.jdbc :as jdbc]
            [openai-api.core :refer :all]
            [docs-qa.vectordb :refer :all])
  (:gen-class))

(defn best-vector-matches [query]
  (clojure.string/join
   " ."
   (let [query-embedding
         (openai-api.core/embeddings query)]
    (map
     second
     (filter
      (fn [emb-text-pair]
        (let [emb (first emb-text-pair)
              text (second emb-text-pair)]
          (> (openai-api.core/dot-product
              query-embedding
              emb)
             0.79)))
      docs-qa.vectordb/embeddings-with-chunk-texts)))))

(defn answer-prompt [prompt]
  (openai-api.core/answer-question
   prompt))

(defn -main
  []
  (println "Loading text files in ./data/, performing chunking and getting OpenAI embeddings...")
  (answer-prompt "do nothing")n ;; force initiation
  (print "...done loading data and getting local embeddings.\n")
  (loop []
  (println "Enter a query:")
  (let [input (read-line)]
    (if (empty? input)
      (println "Done.")
      (do
        (let [text (best-vector-matches input)
              prompt
              (clojure.string/replace
               (clojure.string/join
                "\n"
                ["With the following CONTEXT:\n\n"
                text
                "\n\nANSWER:\n\n"
                input])
               #"\s+" " ")]
          (println "** PROMPT:" prompt)
          (println (answer-prompt prompt)))
          (recur))))))
```

The main example function reads the text files in **./data/**, chunks the files, and uses the OpenAI APIs to get embeddings for each chunk. The main function then has an infinite loop where you can enter a question about your local documents. The most relevant chunks are identified and turned into a prompt along with your question, the the generated prompt and answer to the question are printed. You can enter a control-D to stop the example program:

```Console
$ lein run
Loading text files in ./data/, performing chunking and getting OpenAI embeddings...
...done loading data and getting local embeddings.
Enter a query:
What is Chemistry. How useful, really, are the sciences. Is Amyl alcohol is an organic compound?
PROMPT: With the following CONTEXT: Amyl alcohol is an organic compound with the formula C 5 H 12 O. All eight isomers of amyl alcohol are known. The most important is isobutyl carbinol, this being the chief constituent of fermentation .een 128 and 132 C only being collected. The 1730 definition of the word "chemistry", as used by Georg Ernst Stahl, meant the art of resolving mixed, compound, or aggregate bodies into their principles .; and of composing such bodies from those principles. In 1837, Jean-Baptiste Dumas considered the word "chemistry" to refer to the science concerned with the laws and effects of molecular forces.[16] .This definition further evolved until, in 1947, it came to mean the science of substances: their structure, their properties, and the reactions that change them into other substances - a characterizat .ion accepted by Linus Pauling.[17] More recently, in 1998, the definition of "chemistry" was broadened to mean the study of matter and the changes it undergoes, as phrased by Professor Raymond Chang. .ther aggregates of matter. This matter can be studied in solid, liquid, or gas states, in isolation or in combination. The interactions, reactions and transformations that are studied in chemistry are ANSWER: What is Chemistry. How useful, really, are the sciences. Is Amyl alcohol is an organic compound?

Chemistry is the science of substances: their structure, their properties, and the reactions that change them into other substances. Amyl alcohol is an organic compound with the formula C5H12O. All eight isomers of amyl alcohol
Enter a query:
What is the Austrian School of Economics?
PROMPT: With the following CONTEXT: The Austrian School (also known as the Vienna School or the Psychological School ) is a Schools of economic thought|school of economic thought that emphasizes the spontaneous organizing power of the p .rice mechanism. Austrians hold that the complexity of subjective human choices makes mathematical modelling of the evolving market extremely difficult (or Undecidable and advocate a "laissez faire" ap .proach to the economy. Austrian School economists advocate the strict enforcement of voluntary contractual agreements between economic agents, and hold that commercial transactions should be subject t .o the smallest possible imposition of forces they consider to be (in particular the smallest possible amount of government intervention). The Austrian School derives its name from its predominantly Au .strian founders and early supporters, including Carl Menger, Eugen von Böhm-Bawerk and Ludwig von Mises. Economics is the social science that analyzes the production, distribution, and consumption of .growth, and monetary and fiscal policy. The professionalization of economics, reflected in the growth of graduate programs on the subject, has been described as "the main change in economics since .essional study; see Master of Economics. Economics is the social science that studies the behavior of individuals, households, and organizations (called economic actors, players, or agents), whe . govern the production, distribution and consumption of goods and services in an exchange economy.[3] An approach to understanding these processes, through the study of agent behavior under scarcity, ANSWER: What is the Austrian School of Economics?


The Austrian School of Economics is a school of economic thought that emphasizes the spontaneous organizing power of the price mechanism and advocates a "laissez faire" approach to the economy. It is named after its predominantly Austrian founders and early supporters, including
Enter a query:
Done.
```

## Wrap Up for Using Local Embeddings Vector Database to Enhance the Use of GPT3 APIs With Local Documents

As I write this in May 2023, I have been working almost exclusively with OpenAI APIs for the last year and using the Python libraries for LangChain and LlamaIndex for the last three months.

I started writing the examples in this chapter for my own use, implementing a tiny subset of the LangChain and LlamaIndex libraries in Clojure for creating local embedding vector data stores and for interactive chat using my own data.

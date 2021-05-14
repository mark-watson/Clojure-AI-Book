# Deep Learning Using Deeplearning4j {#dl4j}




One limitation of conventional back propagation neural networks is that they are limited to the number of neuron layers that can be efficiently trained. There are also problems like vanishing gradients (the backpropagated errors that are used to update connection weights) that occur in architectures with many layers.

Deep learning uses computational improvements to mitigate the vanishing gradient problem like using ReLu activation functions rather than the more traditional Sigmoid function, and networks called "skip connections" networks where some layers are initially turned off with connections skipping to the next active layer.

Modern deep learning frameworks like DeepLearning4J, tensorFlow, and PyTorch are easy to use and efficient. We use DeepLearning4J in this chapter because it is written in Java and easy to use with Clojure. In a later chapter we will use the Clojure library **libpython-clj** to access other deep learning-based tools like the Hugging Face Transformer models for question answering systems as well as the **spaCy** Python library for natural language processing (NLP).

I have used GAN (generative adversarial networks) models for synthesizing numeric spreadsheet data, LSTM (long short term memory) models to synthesize highly structured text data like nested JSON, and for NLP (natural language processing). Several of my 55 US patents use neural network and Deep Learning technology.

The [Deeplearning4j.org](http://deeplearning4j.org/) Java library supports many neural network algorithms including support for Deep Learning (DL).  Note that I will often refer to Deeplearning4j as DL4J. 

We will look at a simple example of a feed forward network using the same University of Wisconsin cancer database that we will also use later in the chapel on anomaly detection.

There is a separate [repository of DL4J examples](https://github.com/eclipse/deeplearning4j-examples) that you might want to look at since any of these Java examples that look useful for your projects can be used in Clojure using the example here to get started.

## Feed Forward Classification Networks

Feed forward classification networks are a type of deep neural network that can contain multiple hidden neuron layers. In the example here the adjacent layers are fully connected (all neurons in adjacent layers are connected), as in the examples from the last chapter. The difference here is the use of the DL4J library that is written to scale to large problems and to use GPUs if you have them available.

In general, simpler network architectures are better than unnecessarily complicated architectures. You can start with simple architectures and add layers, different layer types, and parallel models as-needed. For feed forward networks model complexity has two dimensions: the numbers of neurons in hidden layers, and also the number of hidden layers. If you put too many neurons in hidden layers then the training data is effectively memorized and this will hurt performance on data samples not used in training. In practice, I "starve the network" by reducing the number of hidden neurons until the model has reduced accuracy on independent test data. Then I slightly increase the number of neurons in hidden layers. This technique helps avoid models simply memorizing training data.

Our example here reads the University of Wisconsin cancer training and testing data sets, creates a model (lines XXX-YYY), trains it (line ZZZ) and tests it (lines AAA-BBB).

You can increase the number of hidden units in line XXXX (something that you might do for more complex problems). To add a hidden layer you can repeat lines XXX-CCC.


{lang="clojure",linenos=on}
~~~~~~~~
(ns deeplearning-dl4j-clj.wisconsin-data
  (:import [org.datavec.api.split FileSplit]
           [org.deeplearning4j.datasets.datavec 
            RecordReaderDataSetIterator]
           [org.datavec.api.records.reader.impl.csv
            CSVRecordReader]
           [org.deeplearning4j.nn.conf
            NeuralNetConfiguration$Builder]
           [org.deeplearning4j.nn.conf.layers
            OutputLayer$Builder DenseLayer$Builder]
           [org.deeplearning4j.nn.weights WeightInit]
           [org.nd4j.linalg.activations Activation]
           [org.nd4j.linalg.lossfunctions
            LossFunctions$LossFunction]
           [org.deeplearning4j.optimize.listeners
            ScoreIterationListener]
           [org.deeplearning4j.nn.multilayer
            MultiLayerNetwork]
           [java.io File]
           [org.nd4j.linalg.learning.config Adam Sgd
            AdaDelta AdaGrad AdaMax Nadam NoOp]))

(def numHidden 3)
(def numOutputs 1)
(def batchSize 64)

(def initial-seed (long 33117))

(def numInputs 9)
(def labelIndex 9)
(def numClasses 2)


(defn -main
  "Using DL4J with Wisconsin data"
  [& args]
  (let [recordReader (new CSVRecordReader)
        _ (. recordReader
            initialize
            (new FileSplit
             (new File "data/", "training.csv")))
        trainIter
        (new RecordReaderDataSetIterator
          recordReader batchSize labelIndex numClasses)
        recordReaderTest (new CSVRecordReader)
        _ (. recordReaderTest
            initialize
             (new FileSplit
              (new File "data/", "testing.csv")))
        testIter (new RecordReaderDataSetIterator recordReaderTest batchSize labelIndex numClasses)
        conf (->
               (new NeuralNetConfiguration$Builder)
               (.seed initial-seed)
               (.activation Activation/TANH)
               (.weightInit (WeightInit/XAVIER))
               (.updater (new Sgd 0.1))
               (.l2 1e-4)
               (.list)
               (.layer
                 0,
                 (-> (new DenseLayer$Builder)
                     (.nIn numInputs)
                     (.nOut numHidden)
                     (.build)))
               (.layer
                 1,
                 (-> (new OutputLayer$Builder
                       LossFunctions$LossFunction/MCXENT)
                     (.nIn numHidden)
                     (.nOut numClasses)
                     (.activation Activation/SOFTMAX)
                     (.build)))
               (.build))
        model (new MultiLayerNetwork conf)
        score-listener (ScoreIterationListener. 100)]
    (. model init)
    (. model setListeners (list score-listener))
    (. model fit trainIter 10)
    (while (. testIter hasNext)
      (let [ds (. testIter next)
            features (. ds getFeatures)
            labels (. ds getLabels)
            predicted (. model output features false)]
        ;; 26 test samples in data/testing.csv:
        (doseq [i (range 0 52 2)]
          (println
            "desired output: [" (. labels getDouble i)
            (. labels getDouble (+ i 1)) "]"
            "predicted output: ["
            (. predicted getDouble i)
            (. predicted getDouble (+ i 1)) "]"))))))
~~~~~~~~

It is very important to not use training data for testing because performance on recognizing training data should always be good assuming that you have enough memory capacity in a network (i.e., enough hidden units and enough neurons in each hidden layer).

The program output shows the target (correct output) and the output predicted by the trained model:

{line-numbers=off}
~~~~~~~~
target: [ 0.0 1.0 ] predicted : [ 0.16 0.84 ]
target: [ 0.0 1.0 ] predicted : [ 0.39 0.61 ]
target: [ 1.0 0.0 ] predicted : [ 0.91 0.09 ]
target: [ 0.0 1.0 ] predicted : [ 0.16 0.84 ]
target: [ 0.0 1.0 ] predicted : [ 0.16 0.84 ]
target: [ 1.0 0.0 ] predicted : [ 0.96 0.04 ]
target: [ 1.0 0.0 ] predicted : [ 0.95 0.05 ]
target: [ 1.0 0.0 ] predicted : [ 0.95 0.05 ]
target: [ 0.0 1.0 ] predicted : [ 0.21 0.79 ]
target: [ 1.0 0.0 ] predicted : [ 0.94 0.06 ]
target: [ 1.0 0.0 ] predicted : [ 0.92 0.08 ]
target: [ 0.0 1.0 ] predicted : [ 0.16 0.84 ]
target: [ 1.0 0.0 ] predicted : [ 0.94 0.06 ]
target: [ 0.0 1.0 ] predicted : [ 0.16 0.84 ]
target: [ 1.0 0.0 ] predicted : [ 0.96 0.04 ]
target: [ 0.0 1.0 ] predicted : [ 0.17 0.83 ]
target: [ 0.0 1.0 ] predicted : [ 0.17 0.83 ]
target: [ 0.0 1.0 ] predicted : [ 0.16 0.84 ]
target: [ 0.0 1.0 ] predicted : [ 0.16 0.84 ]
target: [ 1.0 0.0 ] predicted : [ 0.95 0.05 ]
target: [ 1.0 0.0 ] predicted : [ 0.94 0.06 ]
target: [ 1.0 0.0 ] predicted : [ 0.92 0.08 ]
target: [ 1.0 0.0 ] predicted : [ 0.96 0.04 ]
~~~~~~~~


## Optional Material: Documentation For Other Types of DeepLearning4J Builtin Layers

The [documentation for the built-in layer classes in DL4J](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/package-tree.html) is probably more than you need for now so let's review the most other types of layers that I sometimes use. In the simple example we used in the last section we used two types of layers:

- [org.deeplearning4j.nn.conf.layers.DenseLayer](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/DenseLayer.html) - maintains connections to all neurons in the previous and next layer, or it is "fully connected."
- [org.deeplearning4j.nn.conf.layers.OutputLayer](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/OutputLayer.html) - has built-in behavior for starting the back propagation calculations back through previous layers.

As you build more deep learning enabled applications, depending on what requirements you have, you will likely need to use at least some of the following Dl4J layer classes:

- [org.deeplearning4j.nn.conf.layers.AutoEncoder](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/AutoEncoder.html) - often used to remove noise from data. Autoencoders work by making the target training output values equal to the input training values while reducing the number of neurons in the AutoEncoding layer. The layer learns a concise representation of data, or "generalizes" data by learning in which features are important.
- [org.deeplearning4j.nn.conf.layers.CapsuleLayer](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/CapsuleLayer.html) - Capsule networks are an attempt to be more efficient versions of convolutional models. Convolutional networks discard position information of detected features while capsule models maintain and use this information.
- [org.deeplearning4j.nn.conf.layers.Convolution1D](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/Convolution1DLayer.html) - one dimensional convolutional layers learn one dimensional feature detectors. Trained layers learn to recognize features but discard the information of where the feature is located. These are often used for data input streams like signal data and word tokens in natural language processing.
- [org.deeplearning4j.nn.conf.layers.Convolution2D](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/Convolution2D.html) - two dimensional convolutional layers learn two dimensional feature detectors. Trained layers learn to recognize features but discard the information of where the feature is located. These are often used for recognizing if a type of object appears inside a picture. Note that features, for example representing a nose or a mouth, are recognized but their location in an input picture does not matter. For example, you could cut up an image of someone's face, moving the ears to the picture center, the mouth to the upper left corner, etc., and the picture would still be predicted to contain a face with some probability because using soft max output layers produces class labels that can be interpreted as probabilities since the values over all output classes sum to the value 1.
- [org.deeplearning4j.nn.conf.layers.EmbeddingLayer](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/EmbeddingLayer.html) - embedding layers are used to transform input data into integer data. My most frequent use of embedding layers is word embedding where each word in training data is assigned an integer value. This data can be "one hot encoded" and in the case of processing words, if there are 5000 unique words in the training data for a classifier, then the embedding layer would have 5001 neurons, one for each word and one to represent all words not in the training data. If the word index (indexing is zero-based) is, for example 117, then the activation value for neuron at index 117 is set to one and all others in the layer are set to zero.
- [org.deeplearning4j.nn.conf.layers.FeedForwardLayer](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/FeedForwardLayer.html) - this is a super class for most specialized types of feed forward layers so reading through the class reference is recommended.
- [org.deeplearning4j.nn.conf.layers.DropoutLayer](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/DropoutLayer.html) - dropout layers are very useful for preventing learning new input patterns from making the network forget previously learned patterns. For each training batch, some fraction of neurons in a dropout layer are turned off and don't update their weights during a training batch cycle. The development of using dropout was key historically for getting deep learning networks to work with many layers and large amounts of training data.
- [org.deeplearning4j.nn.conf.layers.LSTM](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/LSTM.html) - LSTM layers are used to extend the temporal memory of what a layer can remember. LSTM are a refinement of RNN models that use an input window to pass through a data stream and the RNN model can only use what is inside this temporal sampling window.
- [org.deeplearning4j.nn.conf.layers.Pooling1D](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/Pooling1D.html) - a one dimensional pooling layer transforms a longer input to a shorter output by downsampling, i.e., there are fewer output connections than input connections.
- [org.deeplearning4j.nn.conf.layers.Pooling2D](https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/conf/layers/Pooling2D.html) - a two dimensional pooling layer transforms a larger two dimensional array of data input to a smaller output two dimensional array by downsampling.

## Deep Learning Wrap Up

I first used neural networks in the late 1980s for phoneme (speech) recognition, specifically using time delay neural networks and I gave a talk about it at [IEEE First Annual International Conference on Neural Networks San Diego, California June 21-24, 1987](http://ieeexplore.ieee.org/xpl/articleDetails.jsp?reload=true&arnumber=4307059). In the following year I wrote the Backpropagation neural network code that my company used in a bomb detector that we built for the FAA. Back then, neural networks were not widely accepted but in the present time Google, Microsoft, and many other companies are using deep learning for a wide range of practical problems. Exciting work is also being done in the field of natural language processing.

The example in this chapter is simple so you can experiment with it and provided examples for dealing with calling the DL4J Java APIs directly from Clojure.

Later we will look at an example calling directly out to Python code using the **libpython-clj** library to use the spaCy natural language processing library. You can also use the **libpython-clj** library to access libraries like TensorFlow, PyTorch, etc. in your Clojure applications.
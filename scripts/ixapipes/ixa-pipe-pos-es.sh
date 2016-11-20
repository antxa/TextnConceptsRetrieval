#!/bin/sh

## Update the value of the following variables: 
##   - posJar: name of your ixa-pipe-pos jar file
##   - posModel: name of your model
##   - posLemmaModel: name of your lemmatization model
posJar=ixa-pipe-pos-1.5.1-exec.jar
posModel=es-pos-perceptron-autodict01-ancora-2.0.bin
posLemmaModel=es-lemma-perceptron-ancora-2.0.bin
posDir=/ixa-pipes/ixa-pipe-pos/

java -jar ${posDir}/${posJar} tag -m ${posDir}/${posModel} -lm ${posDir}/${posLemmaModel} -l es

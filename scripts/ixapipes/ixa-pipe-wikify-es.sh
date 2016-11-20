#!/bin/sh

## Update the name of your ixa-pipe-wikify jar file (variable wikifyJar)
wikifyJar=ixa-pipe-wikify-1.4.0.jar
wikifyDir=/ixa-pipes/ixa-pipe-wikify/

java -jar ${wikifyDir}/${wikifyJar} -p 2030

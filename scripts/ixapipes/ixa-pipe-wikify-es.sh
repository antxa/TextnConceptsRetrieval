#!/bin/sh

## Update the value of the following variables:
##   - wikifyJar: name of your ixa-pipe-wikify jar file
##   - mapDB: MapDB database files for Spanish crosslingual links
wikifyJar=ixa-pipe-wikify-1.4.0.jar
mapDB=wikipedia-db
wikifyDir=/ixa-pipes/ixa-pipe-wikify/

java -jar ${wikifyDir}/${wikifyJar} -p 2030 -i ${wikifyDir}/${mapDB} -n esEn 

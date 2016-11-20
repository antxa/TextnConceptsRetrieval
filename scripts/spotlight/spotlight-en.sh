#!/bin/sh

java -Dfile.encoding=UTF-8 -Xms8G -Xmx10G -jar -jar /opt/spotlight/dbpedia-spotlight-latest.jar /opt/spotlight/en  http://localhost:2020/rest

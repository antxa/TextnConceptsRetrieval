#!/bin/sh

## Update the name of your ixa-pipe-tok jar file (variable tokJar)
tokJar=ixa-pipe-tok-1.8.5-exec.jar
tokDir=/ixa-pipes/ixa-pipe-tok/

java -jar ${tokDir}/${tokJar} tok -l en --hardParagraph yes

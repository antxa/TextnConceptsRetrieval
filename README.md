TextnConceptsRetrieval
======================

Docker image packaging for TextnConceptsRetrieval.

TextnConceptsRetrieval is a retrieval system based on [SOLR search
platform](http://lucene.apache.org/solr/). It indexes raw text, as
well as concepts derived from it. For retrieval, it uses text
(monolingual retrieval) and concepts (crosslingual retrieval). It is a
RESTful web service using [Spring](https://spring.io/). It uses [IXA
pipes](http://ixa2.si.ehu.es/ixa-pipes/) and [DBpedia
Spotlight](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki)
to get concepts from the text.


Module contents
================

The contents of the module are the following:

    + COPYING	License file
    + Dockerfile	File containing all the commands to build a Docker image
    + docs4indexing	Empty directory in which to place your documents to be indexed (this directory will be mounted in the docker image)
    + index		Directory to store your Solr cores. It contains a already created 'textnconcepts' empty index.
    + ixa-pipes		Directory to place ixa-pipes tools and their models
    + pom.xml		Maven pom file which deals with everything related to compilation and execution of the web service module
    + scripts		Different scripts that will be executed when running a docker container
    + src/		Java source code of the web service module
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/            it contains a jar executable file and other directories



## TABLE OF CONTENTS

1. [INSTALLATION](#installation)
  + [1.1. Install JDK 1.8](#1-install-jdk-18)
  + [1.2. Install MAVEN 3](#2-install-maven-3)
  + [1.3. Get and compile module source code](#3-get-and-compile-module-source-code)
  + [1.4. Get the IXA pipes tools](#4-get-the-ixa-pipes-tools)
  + [1.5. Install Docker](#5-install-docker)
  + [1.6. Build Docker image](#6-build-docker-image)

2. [USAGE](#usage)
  + [2.1. Run the Docker image](#1-run-the-docker-image)
  + [2.2. Check API documentaion](#2-check-api-documentation)
  + [2.3. Index a single document](#3-index-a-single-document)
  + [2.4. Index all the documents in a directory](#4-index-all-the-documents-in-a-directory)
  + [2.5. Retrieve documents](#5-retrieve-documents)
  + [2.6. Get concepts](#6-get-concepts)


## INSTALLATION

If you want to build the application from source, follow the next steps.

If not, you can use the already built image in [docker
hub](https://hub.docker.com/r/antxa/textnconceptsretrieval/). Pull the
docker image and jump to the [USAGE](#usage) section.

````shell
docker pull antxa/textnconceptsretrieval
````


### 1. Install JDK 1.8

If you do not install JDK 1.8 in a default location, you will probably
need to configure the PATH in .bashrc or .bash_profile:

    export JAVA_HOME=/yourpath/local/java18
    export PATH=${JAVA_HOME}/bin:${PATH}


If you use tcsh you will need to specify it in your .login as follows:

    setenv JAVA_HOME /usr/java/java18
    setenv PATH ${JAVA_HOME}/bin:${PATH}


If you re-login into your shell and run the command

    java -version


you should now see that your JDK is 1.8.


### 2. Install MAVEN 3

Download MAVEN 3 from

````shell
wget http://www.apache.org/dyn/closer.cgi/maven/maven-3/3.0.4/binaries/apache-maven-3.0.4-bin.tar.gz
````

Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/home/ragerri/local/apache-maven-3.0.4
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.0.4
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

### 3. Get and compile module source code

````shell
git clone https://github.com/antxa/TextnConceptsRetrieval.git
cd TextnConceptsRetrieval
mvn clean package
````

### 4. Get the IXA pipes tools

In order to get the concepts from the query and documents, IXA pipes
tools are used; namely
[ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok),
[ixa-pipe-pos](https://github.com/ixa-ehu/ixa-pipe-pos) and
[ixa-pipe-wikify](https://github.com/ixa-ehu/ixa-pipe-wikify).

Place the jar files (and models or other files if needed) of these
tools in the corresponding subdirectory under 'ixa-pipes'
folder. Follow the instructions in their respectives github readmes.

Update the name of the jar and models' files in all the scripts used
for running these tools located in 'scripts/ixapipes/' folder.


### 5. Install Docker

Follow the instructions in [https://www.docker.com/](https://www.docker.com/)


### 6. Build Docker image

````shell
docker build . -t antxa/textnconceptsretrieval
````


## USAGE

### 1. Run the Docker image

Use one of the following commands:

````shell
docker run -v $PWD/index:/opt/solr/server/solr/mycores -v $PWD/docs4indexing:/docs4indexing -it -p 8080:8080 -p 8983:8983 antxa/textnconceptsretrieval start-bash
````

or


````shell
docker run -v $PWD/index:/opt/solr/server/solr/mycores -v $PWD/docs4indexing:/docs4indexing -it -p 8080:8080 -p 8983:8983 antxa/textnconceptsretrieval start
````

Using one of the above commands, we create a container and start some
processes on it. Note that:
  + We mount the 'index' and 'docs4indexing' folders in the Docker container. The owner of the 'index' folder has to be 8983 to be accesible by Solr.
  + As two DBpedia Spotlight servers are launched inside the container (one for English and another one for Spanish), at least 14GB of RAM memory are needed.

The difference between these two commands is the last argument
('start-bash' or 'start'). Using the first one, after initializing the
container, a bash prompt into a running docker will be available.

Now Solr server is up listening on port 8983. You can access the Solr
admin interface and the browse interface of the already created
'textnconcepts' index using your browser following these links:

[http://localhost:8983/solr/#/textnconcepts](http://localhost:8983/solr/#/textnconcepts)

[http://localhost:8983/solr/textnconcepts/browse](http://localhost:8983/solr/textnconcepts/browse)

Also RESTful web service is running listening on port 8080.


### 2. Check API documentation

The RESTful API is documented using [Swagger](http://swagger.io/).

Once the docker image is running, you can check the API documentation
using the user interface or the JSON API following the links below:

````shell
http://localhost:8080/swagger-ui.html
http://localhost:8080/v2/api-docs 
````


### 3. Index a single document

`index` POST HTTP request indexes a single document sent in the
request body.

To index a document (text and concepts will be indexed), you just need
to pass a plain text file in the body of the request ('doc'
parameter), and specify the document id and the language ('en' or
'es') as follows:

````shell
curl -F "doc=@file.txt" localhost:8080/index?"id=id1&lang=en"
````

Check all the available parameters for [`index` request in the Swagger
UI](http://localhost:8080/swagger-ui.html#!/application/indexUsingPOST)

Remember that you can check the updated index information on the Solr
admin interface:
[http://localhost:8983/solr/#/textnconcepts](http://localhost:8983/solr/#/textnconcepts)


### 4. Index all the documents in a directory

`indexdir` POST HTTP request indexes documents located in the
'docs4indexing' directory.

**IMPORTANT** The documents to be indexed (files under 'docs4indexing'
  directory) have to have read permissions (rw-rw-r--).

You can index all the documents (text and concepts will be indexed)
under 'docs4indexing' directory like this:

````shell
curl localhost:8080/indexdir?"docsDir=docs4indexing&lang=en"
````

'docsDir' and 'lang' parameters are mandatory (optional values for
language are 'en' or 'es'). Check all the rest parameters for
[`indexdir` request in the Swagger
UI](http://localhost:8080/swagger-ui.html#!/application/indexdirUsingPOST)

Remember that you can check the updated index information on the Solr
admin interface:
[http://localhost:8983/solr/#/textnconcepts](http://localhost:8983/solr/#/textnconcepts)


### 5. Retrieve documents

`query` POST HTTP request retrieves documents given a query. For
monolingual retrieval, the query will be the content of the plain text
file sent in the request body. For crosslingual retrieval, concepts
extracted from the text will be added to the text query.

To retrieve documents, you just need to pass a plain text file in the
body of the request ('qfile' parameter), and specify the language
('en' or 'es') and the type of retrieval ('mono' or 'cross') as
follows:

````shell
curl -F "qfile=@query.txt" localhost:8080/query?"lang=en&type=cross"
````

If you want, you can specify the number of documents to be retrieved
using 'ndocs' parameter as follows:

````shell
curl -F "qfile=@query.txt" localhost:8080/query?"lang=en&type=cross&ndocs=100"
````

Check all the available parameters for [`query` request in the Swagger
UI](http://localhost:8080/swagger-ui.html#!/application/queryUsingPOST)


### 6. Get concepts

`concepts` POST HTTP request returns a list of concepts derived from
the document sent in the request body.

To get concepts, you just need to pass a plain text file in the body of the request ('doc' parameter) and specify the language ('en' or 'es') as follows:

````shell
curl -F "doc=@file.txt" localhost:8080/concepts?lang=en
````

Check all the available parameters for [`concepts` request in the Swagger
UI](http://localhost:8080/swagger-ui.html#!/application/conceptsUsingPOST)



## Contact information


````shell
Arantxa Otegi
IXA NLP Group
University of the Basque Country (UPV/EHU)
arantza.otegi@ehu.eus
````


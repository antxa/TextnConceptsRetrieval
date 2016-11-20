TextnConceptsRetrieval
======================

Docker image packaging for TextnConceptsRetrieval.

TextnConceptsRetrieval is a retrieval system based on [SOLR search
platform](http://lucene.apache.org/solr/). It allows to index raw
text, as well as concepts. When querying, it is possible to use raw
text or concepts. It is a RESTful web service using
[Spring](https://spring.io/). It uses [IXA
pipes](http://ixa2.si.ehu.es/ixa-pipes/) and [DBpedia
Spotlight](https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki)
to get concepts from the text.


Contents
========

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


1. INSTALLATION
===============

1.1. Install JDK 1.8
--------------------

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


1.2. Install MAVEN 3
--------------------

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

1.3. Get and compile module source code
---------------------------------------

````shell
git clone https://github.com/antxa/TextnConceptsRetrieval.git
cd TextnConceptsRetrieval
mvn clean package
````

1.4. Get the IXA pipes tools
----------------------------

In order to get the concepts from the query and documents, IXA pipes
tools are used; namely
[ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok),
[ixa-pipe-pos](https://github.com/ixa-ehu/ixa-pipe-pos) and
[ixa-pipe-wikify](https://github.com/ixa-ehu/ixa-pipe-wikify).

Place the jar files (and models if needed) of these tools in the
corresponding subdirectory under 'ixa-pipes' folder. Follow the
instructions in their respectives github readmes.

Update the name of the jar and models' files in all the scripts used
for running these tools located in 'scripts/ixapipes/' folder.


1.5. Install Docker
-------------------
Follow the instructions in [https://www.docker.com/](https://www.docker.com/)


1.6. Build Docker image
-----------------------

````shell
docker build . -t textnconceptsretrieval
````


2. USAGE
========

2.1. Run the Docker image
----------------------------

Use one of the following commands:

````shell
docker run -v $PWD/index:/opt/solr/server/solr/mycores -v $PWD/docs4indexing:/docs4indexing -it -p 8080:8080 -p 8983:8983 textnconceptsretrieval start-bash
````

or


````shell
docker run -v $PWD/index:/opt/solr/server/solr/mycores -v $PWD/docs4indexing:/docs4indexing -it -p 8080:8080 -p 8983:8983 textnconceptsretrieval start
````

Using one of the above commands, we create a container and start some
processes on it. Note that we mounted the 'index' and 'docs4indexing'
folders in the Docker container. The owner of the 'index' folder has
to be 8983 to be accesible by Solr.

The difference between these two commans is the last argument
('start-bash' or 'start'). Using the first one, after initializing the
container, a bash prompt into a running docker will be available.

Now Solr server is up listening on port 8983. You can access using
your navigator the Solr admin interface and the browse interface of
the already created 'textnconcepts' index here:

[http://localhost:8983/solr/#/textnconcepts](http://localhost:8983/solr/#/textnconcepts)

[http://localhost:8983/solr/textnconcepts/browse](http://localhost:8983/solr/textnconcepts/browse)

Also RESTful web service is running listening on port 8080.


2.2. Indexing a document
------------------------

'index' POST/GET HTTP request indexes documents located in the
'docs4indexing' directory.

**IMPORTANT** The documents to be indexed (files under 'docs4indexing'
  directory) have to have read permissions (rw-rw-r--).

Use this command to index all the documents under 'docs4indexing'
directory (only raw text will be indexed, no concepts):

````shell
curl localhost:8080/index?docsDir=docs4indexing
````

If you want to index also concepts extracted from the documents, use
the type parameter as follows:

````shell
curl localhost:8080/index?"dosscsDir=docs4indexing&type=concepts
````

Check all the available parameters for this request in the
'src/main/java/textncoceptsretrieval/Application.java' source file.

Remember that you can check the updated index information on the Solr
admin interface:
[http://localhost:8983/solr/#/textnconcepts](http://localhost:8983/solr/#/textnconcepts)


2.3. Retrieving documents
-------------------------

'query' POST HTTP request retrieves a documents given a query (text
document file).

Use this command to retrieve documents using the text in 'query.txt'
file as an input query: directory (only raw text will be indexed, no
concepts):

````shell
curl -F "qfile=@query.txt" localhost:8080/query
````

If you want, you can specify the number of documents to be retrieved
using 'ndocs' parameter as follows:

````shell
curl -F "qfile=@query.txt" localhost:8080/query&ndocs=1000
````

Check all the available parameters for this request in the
'src/main/java/textncoceptsretrieval/Application.java' source file.



Contact information
===================

````shell
Arantxa Otegi
IXA NLP Group
University of the Basque Country (UPV/EHU)
arantza.otegi@ehu.eus
````


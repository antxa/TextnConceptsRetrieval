TextnConceptsRetrieval
======================

TextnConceptsRetrieval is a retrieval system based on SOLR search
platform. It allows to index raw text, as well as concepts. When
querying, it is possible to use raw text or concepts.


Contents
========

The contents of the module are the following:

    + pom.xml                 maven pom file which deals with everything related to compilation and execution of the module
    + src/                    java source code of the module
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/                 it contains binary executable and other directories


INSTALLATION
============

1. Install MAVEN 3
------------------

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

3. Get module source code
--------------------------

````shell
git clone https://github.com/antxa/TextnConceptsRetrieval.git
cd TextnConceptsRetrieval
mvn clean package
````

4. Usage
========

````shell
java -jar target/TextnConceptsRetrieval-$version.jar -help
````


Contact information
===================

````shell
Arantxa Otegi
IXA NLP Group
University of the Basque Country (UPV/EHU)
arantza.otegi@ehu.eus
````


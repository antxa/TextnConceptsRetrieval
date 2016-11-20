#!/bin/bash
#
# A helper script to initialise a custom SOLR_HOME.
# For example:
#
#    mkdir mysolrhome
#    sudo chown 8983:8983 mysolrhome
#    docker run -it -v $PWD/mysolrhome:/mysolrhome -e SOLR_HOME=/mysolrhome -e INIT_SOLR_HOME=yes solr
#

if [[ "$VERBOSE" = "yes" ]]; then
    set -x
fi

# Normally SOLR_HOME is not set, and Solr will use /opt/solr/server/solr/
# If it's not set, then this script has no relevance.
if [[ -z $SOLR_HOME ]]; then
    exit
fi

# require an explicit opt-in. Without this, you can use the SOLR_HOME and
# volumes, and configure it from the command-line or a docker-entrypoint-initdb.d
# script
if [[ "$INIT_SOLR_HOME" != "yes" ]]; then
  exit
fi

# check the directory exists.
if [ ! -d "$SOLR_HOME" ]; then
    echo "SOLR_HOME $SOLR_HOME does not exist"
    exit 1
fi

# don't do anything if it is non-empty
if [ $(find "$SOLR_HOME" -mindepth  1 | wc -l) != '0' ]; then
    exit
fi

# populate with default solr home contents
echo "copying solr home contents to $SOLR_HOME"
cp -R /opt/solr/server/solr/*  "$SOLR_HOME"

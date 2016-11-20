#!/bin/bash
set -e


INIT_LOG=${INIT_LOG:-/opt/docker-solr/init.log}

# configure Solr to run on the local interface, and start it running
# in the background
function initial_solr_begin {
    echo "Configuring Solr to bind to 127.0.0.1"
    cp /opt/solr/bin/solr.in.sh /opt/solr/bin/solr.in.sh.orig
    echo "SOLR_OPTS=-Djetty.host=127.0.0.1" >> /opt/solr/bin/solr.in.sh
    echo "Running solr in the background. Logs are in /opt/solr/server/logs"
    /opt/solr/bin/solr start
    max_try=${MAX_TRY:-12}
    wait_seconds=${WAIT_SECONDS:-5}
    if ! /opt/docker-solr/scripts/wait-for-solr.sh "$max_try" "$wait_seconds"; then
        echo "Could not start Solr."
        if [ -f /opt/solr/server/logs/solr.log ]; then
            echo "Here is the log:"
            cat /opt/solr/server/logs/solr.log
        fi
        exit 1
    fi
}

# stop the background Solr, and restore the normal configuration
function initial_solr_end {
    echo "Shutting down the background Solr"
    /opt/solr/bin/solr stop
    echo "Restoring Solr configuration"
    mv /opt/solr/bin/solr.in.sh.orig /opt/solr/bin/solr.in.sh
    echo "Running Solr in the foreground"
}

function init_actions {
    # init script for handling a custom SOLR_HOME
    /opt/docker-solr/scripts/init-solr-home.sh

    # execute files in /docker-entrypoint-initdb.d before starting solr
    # for an example see docs/set-heap.sh
    shopt -s nullglob
    for f in /docker-entrypoint-initdb.d/*; do
        case "$f" in
            *.sh)     echo "$0: running $f"; . "$f" ;;
            *)        echo "$0: ignoring $f" ;;
        esac
        echo
    done
}



if [[ "$1" = 'start-bash' ]]; then
    echo 'Initializing DBpedia Spotlight server (English model).....'
    spotlight-en.sh &
    echo 'Initializing DBpedia Spotlight server (Spanish model).....'
    spotlight-es.sh &
    echo 'Initializing SOLR.....'
    init_actions
    solr start
    echo 'Initializing Spring.....'
    /usr/lib/jvm/java-8-openjdk-amd64/bin/java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar TextnConceptsRetrieval.jar &
    /bin/bash

elif [[ "$1" = 'start' ]]; then
    echo 'Initializing DBpedia Spotlight server (English model).....'
    spotlight-en.sh &
    echo 'Initializing DBpedia Spotlight server (Spanish model).....'
    spotlight-es.sh &
    echo 'Initializing SOLR.....'
    init_actions
    solr start
    echo 'Initializing Spring.....'
    /usr/lib/jvm/java-8-openjdk-amd64/bin/java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar TextnConceptsRetrieval.jar 

else
    echo 'Wrong command. Options: "start" or "start-bash"'
fi


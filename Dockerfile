FROM openjdk:8-jdk


### DBPEDIA-SPOTLIGHT ##############

ENV RELEASE_SERVER    spotlight.sztaki.hu
ENV RELEASE_FILENAME  dbpedia-spotlight-latest.jar
ENV LANGUAGE_MODEL_EN    en.tar.gz
ENV LANGUAGE_MODEL_ES    es.tar.gz

RUN mkdir -p /opt/spotlight && \
    cd /opt/spotlight && \
    curl -O "http://$RELEASE_SERVER/downloads/$RELEASE_FILENAME" && \
    curl -O "http://$RELEASE_SERVER/downloads/latest_models/$LANGUAGE_MODEL_EN" && \
    curl -O "http://$RELEASE_SERVER/downloads/latest_models/$LANGUAGE_MODEL_ES" && \	
    tar xvf $LANGUAGE_MODEL_EN  && \
    tar xvf $LANGUAGE_MODEL_ES  && \
    rm  $LANGUAGE_MODEL_EN  && \
    rm  $LANGUAGE_MODEL_ES

COPY scripts/spotlight/spotlight-en.sh /bin/spotlight-en.sh
COPY scripts/spotlight/spotlight-es.sh /bin/spotlight-es.sh
RUN chmod +x /bin/spotlight-en.sh
RUN chmod +x /bin/spotlight-es.sh

EXPOSE 8080
EXPOSE 8081


### IXA-PIPES ############

COPY scripts/ixapipes/* /bin/
COPY ixa-pipes/ /ixa-pipes
RUN chmod +r -R /ixa-pipes


### SOLR #################

# Override the solr download location with e.g.:
#   docker build -t mine --build-arg SOLR_DOWNLOAD_SERVER=http://www-eu.apache.org/dist/lucene/solr .
ARG SOLR_DOWNLOAD_SERVER

# Override the GPG keyserver with e.g.:
#   docker build -t mine --build-arg GPG_KEYSERVER=hkp://eu.pool.sks-keyservers.net .
ARG GPG_KEYSERVER

RUN apt-get update && \
  apt-get -y install lsof && \
  rm -rf /var/lib/apt/lists/*

ENV SOLR_USER solr
ENV SOLR_UID 8983

RUN groupadd -r -g $SOLR_UID $SOLR_USER && \
  useradd -r -u $SOLR_UID -g $SOLR_USER $SOLR_USER

ENV SOLR_KEY 38D2EA16DDF5FC722EBC433FDC92616F177050F6
ENV GPG_KEYSERVER ${GPG_KEYSERVER:-hkp://ha.pool.sks-keyservers.net}
RUN gpg --keyserver "$GPG_KEYSERVER" --recv-keys "$SOLR_KEY"

ENV SOLR_VERSION 6.2.1
ENV SOLR_SHA256 344cb317ab42978dcc66944dd8cfbd5721e27e1c64919308082b0623a310b607
ENV SOLR_URL ${SOLR_DOWNLOAD_SERVER:-https://archive.apache.org/dist/lucene/solr}/$SOLR_VERSION/solr-$SOLR_VERSION.tgz

RUN mkdir -p /opt/solr && \
  wget -nv $SOLR_URL -O /opt/solr.tgz && \
  wget -nv $SOLR_URL.asc -O /opt/solr.tgz.asc && \
  echo "$SOLR_SHA256 */opt/solr.tgz" | sha256sum -c - && \
  (>&2 ls -l /opt/solr.tgz /opt/solr.tgz.asc) && \
  gpg --batch --verify /opt/solr.tgz.asc /opt/solr.tgz && \
  tar -C /opt/solr --extract --file /opt/solr.tgz --strip-components=1 && \
  rm /opt/solr.tgz* && \
  rm -Rf /opt/solr/docs/ && \
  mkdir -p /opt/solr/server/solr/lib /opt/solr/server/solr/mycores && \
  sed -i -e 's/#SOLR_PORT=8983/SOLR_PORT=8983/' /opt/solr/bin/solr.in.sh && \
  sed -i -e '/-Dsolr.clustering.enabled=true/ a SOLR_OPTS="$SOLR_OPTS -Dsun.net.inetaddr.ttl=60 -Dsun.net.inetaddr.negative.ttl=60"' /opt/solr/bin/solr.in.sh && \
  chown -R $SOLR_USER:$SOLR_USER /opt/solr && \
  mkdir /docker-entrypoint-initdb.d /opt/docker-solr/

COPY scripts/solr /opt/docker-solr/scripts
RUN chown -R $SOLR_USER:$SOLR_USER /opt/docker-solr

ENV PATH /opt/solr/bin:/opt/docker-solr/scripts:$PATH

EXPOSE 8983
USER $SOLR_USER


### RETRIEVAL - SPRING ######################

VOLUME /tmp

COPY target/TextnConceptsRetrieval-2.0.0.jar TextnConceptsRetrieval.jar
COPY scripts/docker-entrypoint.sh /

ENV JAVA_OPTS=""
ENTRYPOINT ["/bin/bash","/docker-entrypoint.sh"]
CMD ["start-bash"]


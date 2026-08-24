#
# Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
#
# Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#

FROM sonatype.repo.sonatype.app/docker-all/amazonlinux:2023

ENV GROOVY_HOME=/usr/share/groovy \
    GROOVY_VERSION=3.0.25

# make sure the used shell is bash
SHELL ["/bin/bash", "-c"]
ENV SHELL=/bin/bash

RUN echo "Install packages" && \
    dnf install -y java-17-amazon-corretto-devel git wget unzip openssh-clients shadow-utils findutils && \
    dnf clean all && \
    echo "Set JAVA_HOME" && \
    echo "export JAVA_HOME=\$(dirname \$(dirname \$(readlink -f \$(which java))))" >> /etc/profile.d/java.sh && \
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) && \
    echo "Add jenkins user and home directory" && \
    groupadd -g 100 users || true && \
    useradd -u 1002 -g 100 -m -d /home/jenkins jenkins && \
    echo "Install groovy" && \
    wget -q -O /tmp/groovy.zip "https://groovy.jfrog.io/artifactory/dist-release-local/groovy-zips/apache-groovy-binary-${GROOVY_VERSION}.zip" && \
    unzip -o -d "/tmp" "/tmp/groovy.zip" && \
    mv "/tmp/groovy-${GROOVY_VERSION}" "/usr/share/groovy" && \
    rm /tmp/groovy.zip

ENV JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto

COPY --chown=jenkins:100 home /home/jenkins
RUN chmod 700 /home/jenkins/.ssh/

ENV PATH="$JAVA_HOME/bin:$PATH:$GROOVY_HOME/bin"

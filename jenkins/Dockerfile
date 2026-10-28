# Custom Jenkins controller image for this project:
#   Jenkins LTS + Maven + Docker CLI + Docker Compose plugin + required plugins.
# Builds/containers run on the HOST Docker engine through the mounted
# /var/run/docker.sock ("Docker-outside-of-Docker").
FROM jenkins/jenkins:lts-jdk21

USER root

# Maven for the Build/Test stages, Docker CLI + Compose v2 plugin from Docker's apt repo
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl gnupg maven \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && chmod a+r /etc/apt/keyrings/docker.asc \
 && . /etc/os-release \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian ${VERSION_CODENAME} stable" \
      > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin docker-buildx-plugin \
 && rm -rf /var/lib/apt/lists/*

# Plugins used by the Jenkinsfile (installed at image build time)
RUN jenkins-plugin-cli --plugins \
    workflow-aggregator \
    git \
    github \
    pipeline-stage-view \
    junit \
    coverage \
    email-ext \
    mailer \
    credentials-binding \
    timestamper \
    ws-cleanup

USER jenkins

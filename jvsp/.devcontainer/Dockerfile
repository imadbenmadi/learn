# Image for the "dev" service. Built on the HOST by devcontainer.json's initializeCommand
# (`podman build`), NOT by Compose: Compose v5 delegates every build to BuildKit/Bake, which the
# Podman engine does not provide. Podman builds with buildah, which handles this plain Dockerfile.
#
# Base: Microsoft's dev container Java image (Debian trixie, JDK 25 via SDKMAN, non-root user
# "vscode" with uid 1000 and passwordless sudo, git preinstalled). The "3-" prefix pins the image's
# own major version so a breaking change in the image layout cannot surprise a rebuild.
FROM mcr.microsoft.com/devcontainers/java:3-25-trixie

ARG MAVEN_VERSION=3.10.2
ARG NODE_MAJOR=26

SHELL ["/bin/bash", "-o", "pipefail", "-c"]
USER root

# docker-cli: only a client. It talks to the Podman engine through the mounted socket and is used
#             for debugging and by smoke-test.sh. Testcontainers itself does not need it.
# postgresql-client: psql / pg_isready for poking at the database from the terminal.
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      docker-cli postgresql-client xz-utils ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*

# Apache Maven, checksum-verified. Each project ships its own Maven Wrapper (./mvnw) pinned to a
# version; the global `mvn` is only needed to generate those wrappers.
RUN base="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries" \
 && file="apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
 && curl -fsSLo "/tmp/${file}" "${base}/${file}" \
 && echo "$(curl -fsSL "${base}/${file}.sha512" | awk '{print $1}')  /tmp/${file}" | sha512sum -c - \
 && mkdir -p /opt/maven \
 && tar -xzf "/tmp/${file}" -C /opt/maven --strip-components=1 \
 && ln -s /opt/maven/bin/mvn /usr/local/bin/mvn \
 && rm "/tmp/${file}"

# Node.js (latest release of the current LTS major), checksum-verified against nodejs.org.
RUN case "$(dpkg --print-architecture)" in amd64) arch=x64 ;; arm64) arch=arm64 ;; *) exit 1 ;; esac \
 && base="https://nodejs.org/dist/latest-v${NODE_MAJOR}.x" \
 && curl -fsSLo /tmp/SHASUMS256.txt "${base}/SHASUMS256.txt" \
 && file="$(grep -oE "node-v[0-9.]+-linux-${arch}\.tar\.xz$" /tmp/SHASUMS256.txt)" \
 && curl -fsSLo "/tmp/${file}" "${base}/${file}" \
 && (cd /tmp && grep " ${file}\$" SHASUMS256.txt | sha256sum -c -) \
 && tar -xJf "/tmp/${file}" -C /usr/local --strip-components=1 --no-same-owner \
      --exclude='*.md' --exclude='LICENSE' \
 && rm "/tmp/${file}" /tmp/SHASUMS256.txt

# Access to the container engine socket. The bind-mounted socket keeps the Podman machine's
# owner/group ids (mode 0660), and its group id is only known at runtime. This script, run as the
# container's root entrypoint step (see compose.yaml), adds "vscode" to that group. It is no
# escalation: "vscode" already has passwordless sudo in this image.
COPY --chmod=755 docker-socket-access.sh /usr/local/bin/docker-socket-access.sh

# Mount points for the named volumes (Maven repository, Claude Code config). They are created
# here, owned by vscode, because an empty named volume is initialised from the image directory it
# is mounted over, including ownership. Without this the volumes would be owned by root.
RUN install -d -o vscode -g vscode /home/vscode/.m2 /home/vscode/.claude

USER vscode

# Claude Code via Anthropic's official native installer (installs to ~/.local/bin and
# auto-updates). Anthropic's Dev Container Feature is the other official route, but features are
# built with BuildKit-only Dockerfile syntax, which is unreliable on Podman's builder.
RUN curl -fsSL https://claude.ai/install.sh | bash

ENV PATH="/home/vscode/.local/bin:/opt/maven/bin:${PATH}" \
    MAVEN_HOME=/opt/maven

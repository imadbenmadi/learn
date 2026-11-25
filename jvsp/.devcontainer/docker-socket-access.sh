#!/usr/bin/env bash
# Container entrypoint step, runs as root BEFORE VS Code attaches, then hands over to "$@".
#
# The engine socket is bind-mounted from the Podman machine with its original owner and group
# ids. Its group id is whatever the machine's distro uses (gid 10 "wheel" on the Fedora-based
# Podman machine, which shows up here as Debian's "uucp"). It cannot be known when the image is
# built, so it is read here and the "vscode" user is added to a group with that id. Processes
# that VS Code starts afterwards (terminals, the Java language server, Maven) get that group.
set -euo pipefail

socket=/var/run/docker.sock
user=vscode

if [[ -S "$socket" ]]; then
  gid="$(stat -c %g "$socket")"
  if ! id -G "$user" | tr ' ' '\n' | grep -qx "$gid"; then
    group="$(getent group "$gid" | cut -d: -f1 || true)"
    if [[ -z "$group" ]]; then
      group=docker-host
      groupadd --gid "$gid" "$group"
    fi
    usermod -aG "$group" "$user"
  fi
fi

exec "$@"

#!/usr/bin/env bash
# Verifies the dev container: toolchain, the container engine socket, the networking that
# Testcontainers depends on, and the postgres service. Run inside the dev container:
#   bash .devcontainer/smoke-test.sh
set -uo pipefail

failures=0
pass()    { printf '  \033[32mPASS\033[0m %s\n' "$1"; }
fail()    { printf '  \033[31mFAIL\033[0m %s\n' "$1"; failures=$((failures + 1)); }
section() { printf '\n== %s\n' "$1"; }

section "Java"
if java -version 2>&1 | sed 's/^/    /'; then
  java -version 2>&1 | grep -q 'version "25' && pass "JDK 25" || fail "java is not version 25"
else
  fail "java not found"
fi

section "Maven"
if mvn -version 2>&1 | sed 's/^/    /'; then
  pass "mvn runs"
else
  fail "mvn not found"
fi

section "Other tools"
for tool in git node npm claude psql docker; do
  if version="$("$tool" --version 2>&1 | head -n1)"; then
    pass "$tool: $version"
  else
    fail "$tool not found"
  fi
done

section "Container engine socket (${DOCKER_HOST:-unset})"
socket="/var/run/docker.sock"
if [[ ! -S "$socket" ]]; then
  fail "$socket is not a socket (mount missing in compose.yaml?)"
elif [[ "$(curl -fsS --max-time 5 --unix-socket "$socket" http://engine/_ping 2>&1)" == "OK" ]]; then
  pass "engine answers /_ping"
  docker version --format '    server: {{.Server.Version}} (API {{.Server.APIVersion}})' 2>/dev/null \
    || docker version 2>&1 | sed 's/^/    /'
else
  fail "no answer on $socket; permissions: $(stat -c '%U:%G %a' "$socket"), my groups: $(id -Gn)"
fi

section "Sibling container port (how Testcontainers reaches its containers)"
host="${TESTCONTAINERS_HOST_OVERRIDE:-localhost}"
# Pull separately so pull progress (stderr) never ends up in the container id.
docker pull -q docker.io/library/alpine:3 >/dev/null 2>&1
if ! cid="$(docker run -d --rm -p 8080 docker.io/library/alpine:3 \
              sh -c 'while true; do echo pong | nc -l -p 8080; done' 2>/tmp/smoke-run.err)"; then
  fail "could not start a container: $(cat /tmp/smoke-run.err)"
else
  port="$(docker port "$cid" 8080/tcp | head -n1 | sed 's/.*://')"
  reply=""
  for _ in $(seq 1 10); do
    reply="$(timeout 3 bash -c "exec 3<>/dev/tcp/${host}/${port} && read -t 2 line <&3 && echo \"\$line\"" 2>/dev/null)"
    [[ "$reply" == "pong" ]] && break
    sleep 1
  done
  [[ "$reply" == "pong" ]] && pass "reached ${host}:${port}" || fail "could not reach ${host}:${port}"
  docker rm -f "$cid" >/dev/null 2>&1
fi

section "Socket mount for Ryuk (${TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE:-unset})"
override="${TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE:-/var/run/docker.sock}"
docker pull -q docker.io/curlimages/curl:8.11.1 >/dev/null 2>&1
# --user 0: Ryuk runs as root; the curl image would otherwise run as a non-root user that the
# socket's 0660 mode rejects, which says nothing about Ryuk.
if out="$(docker run --rm --privileged --user 0 -v "${override}:/var/run/docker.sock" \
            docker.io/curlimages/curl:8.11.1 -fsS --max-time 5 --unix-socket /var/run/docker.sock http://engine/_ping 2>&1)" \
   && [[ "$out" == "OK" ]]; then
  pass "a sibling container can use the engine socket"
else
  fail "a sibling container cannot use ${override}: $out"
fi

section "PostgreSQL (${PGHOST:-unset}:${PGPORT:-unset})"
if pg_isready -t 5 >/dev/null 2>&1; then
  pass "pg_isready"
  psql -tAc 'select version()' 2>&1 | sed 's/^/    /'
else
  fail "postgres not reachable"
fi

echo
if (( failures == 0 )); then
  echo "All checks passed."
else
  echo "$failures check(s) failed. See Troubleshooting in README.md."
  exit 1
fi

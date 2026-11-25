# jvsp: Spring Boot learning monorepo

- The task and all rules are in CURRICULUM.md. Follow its Workflow section.
- The user knows Java, Node.js/TypeScript, MongoDB and MySQL well. Do not explain basic Java.
  Do explain what Spring does under the hood (auto-configuration, beans, proxies, transactions,
  the security filter chain).
- Versions and the reasons for them are in README.md ("Chosen versions"). Spring Boot 4.2.0,
  Java 25, Maven 3.10.2 (projects use the Maven Wrapper), PostgreSQL 19.

## Environment (already working, do not redesign it)

You run inside the dev container (Debian, user `vscode`) on a ROOTFUL Podman machine (WSL2) on
Windows. Everything about it is explained in README.md and the comments in .devcontainer/.
Hard-won facts, each one verified:

- The image is built by `podman build` (devcontainer.json initializeCommand), never by Compose:
  Compose v5 needs BuildKit, which Podman lacks. No Dev Container Features for the same reason.
- Engine socket: the VM's /run/podman/podman.sock is mounted at /var/run/docker.sock. Its group is
  gid 10; .devcontainer/docker-socket-access.sh adds `vscode` to that gid at container start.
- Testcontainers env vars are set in compose.yaml. TESTCONTAINERS_HOST_OVERRIDE is the compose
  network gateway 10.89.200.1 (subnet pinned in compose.yaml). host.containers.internal does NOT
  work: it points at the Windows side. Ryuk stays enabled (rootful).
- PostgreSQL for local runs: host `postgres`, port 5432, user/password/db `dev`/`dev`/`dev`
  (dev-only). Integration tests use Testcontainers, not this instance.
- Check the environment with `bash .devcontainer/smoke-test.sh`.
- Redis and RabbitMQ (projects 04 and 05) are added as services to .devcontainer/compose.yaml,
  on the same network.

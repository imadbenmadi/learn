# jvsp: Spring Boot learning monorepo

- The task and all rules are in CURRICULUM.md. Follow its Workflow section.
- The user knows Java, Node.js/TypeScript, MongoDB and MySQL well. Do not explain basic Java.
  Do explain what Spring does under the hood (auto-configuration, beans, proxies, transactions,
  the security filter chain).
- Versions and the reasons for them are in README.md ("Chosen versions"). Spring Boot 4.2.0,
  Java 25, Maven 3.10.2 (projects use the Maven Wrapper), PostgreSQL 19.

## Frontend rule (every project)

Every project gets an Angular frontend in `NN-.../frontend/` that exercises the project's API, so
the user can click through what the backend does instead of only using curl.

- Latest GA Angular (22.3 as of 2026-11-26), generated with the Angular CLI, on the container's
  Node 26. Modern Angular only: standalone components, signals (`signal`, `computed`, `resource`),
  zoneless change detection, `inject()`, built-in control flow (`@if`, `@for`), typed reactive
  forms, `OnPush`, lazy-loaded routes. No NgModules, no `any`, no Angular Material unless a project
  needs it (plain SCSS keeps the focus on Spring).
- Package-by-feature like the backend (`src/app/<feature>/`), one typed API service per backend
  resource whose TypeScript types mirror the backend records.
- Show backend errors properly: parse `application/problem+json` (RFC 9457) and display `title`,
  `detail` and field `errors`.
- Dev setup: `npm start` serves on :4200 with `proxy.conf.json` forwarding `/api` and `/actuator`
  to :8080, so there is no CORS in development (CORS is taught in project 03, not worked around
  earlier). The frontend stays a separate npm project; it is not built by Maven.
- Tests with the CLI's default runner (Vitest). `npm test` and `npm run build` must pass, like
  `./mvnw verify` does for the backend.
- The project README gets a short "Frontend" section: how to run it and which Spring behaviour each
  screen demonstrates. Do not teach Angular itself; the user knows TypeScript.

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

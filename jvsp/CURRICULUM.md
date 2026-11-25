## PHASE 2 — only inside the dev container
Start by running .devcontainer/smoke-test.sh. If anything fails (especially Testcontainers on
Podman), fix the dev container setup first and explain the fix.

Monorepo layout: /01-..., /02-..., etc. Each folder is a standalone Spring Boot application using
Maven with the Maven Wrapper. No Lombok: use records and modern Java (pattern matching, sealed
types, text blocks) where it fits naturally.

01-rest-fundamentals — Task API with in-memory storage
  Controllers, request/response DTOs as records, Bean Validation, global error handling with
  ProblemDetail (RFC 9457), @ConfigurationProperties and profiles, logging, the application
  context and dependency injection explained. Tests: web slice tests and unit tests.

02-data-jpa-postgres — Inventory and orders on PostgreSQL
  Spring Data JPA, entity relationships, Flyway migrations, @Transactional (and why
  self-invocation breaks it), pagination and sorting, the N+1 problem shown and then fixed,
  projections, optimistic locking, the same query written with JdbcClient for comparison.
  Integration tests with Testcontainers.

03-security — Authentication and authorisation for an API
  The security filter chain explained, users and roles stored in PostgreSQL, password hashing,
  stateless JWT via OAuth2 Resource Server (self-issued keys for local development), method
  security, CORS, CSRF (when it matters and when it does not), security tests.

04-production-concerns — Order processing service
  HTTP interface clients calling an external API, timeouts/retries/resilience, @Async and
  virtual threads, scheduled jobs, Redis caching, RabbitMQ producer and consumer with an
  idempotent consumer and a dead-letter queue, Actuator and Micrometer metrics, structured
  logging, graceful shutdown. Testcontainers for Redis and RabbitMQ.

05-capstone-mini-erp — Modular monolith
  Customers, products, orders and invoices as Spring Modulith modules (enforced boundaries,
  application events between modules), OpenAPI with springdoc, one database schema per module,
  a full test suite, a multi-stage non-root Dockerfile using the layered jar, and a GitHub
  Actions workflow running ./mvnw verify.

Rules for every project:
- Production-quality code: package-by-feature, constructor injection only, meaningful names,
  no dead code, no TODOs.
- Comments explain the Spring-specific "why", not what the Java code does.
- Each project has a README.md written like a book chapter: goals, concepts with Mermaid
  diagrams, how a request flows through Spring, how to run it, 3–5 exercises for me
  (hints only, no solutions), and "read next" links to the exact official documentation sections.
- ./mvnw verify must pass inside the container before moving on.

Workflow:
- First show me the plan for PHASE 2 (structure, versions, dependencies per project) and wait
  for my OK.
- Then build ONE project at a time. After each one: run the tests, tell me which files to study
  and in what order, and stop until I say "next".
- If something is ambiguous, choose what real companies most commonly do and tell me why.

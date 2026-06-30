# MLOps Pipeline Management API

A RESTful service built with **JAX-RS (Jersey 2.x)** for managing Machine Learning
Workspaces, the Models deployed within them, and the historical Evaluation Metrics
recorded for each model. State is held entirely in memory (HashMaps / ArrayLists) —
no database is used, as required by the coursework.

> Module: 5COSC022W – Client-Server Architectures · Technology stack: **JAX-RS only**.

---

## 1. API Design Overview

The API is versioned under `/api/v1` and exposes three primary resource collections,
plus a root discovery endpoint:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1` | Discovery endpoint – API metadata + links to resource collections |
| `GET` | `/api/v1/workspaces` | List all workspaces |
| `POST` | `/api/v1/workspaces` | Create a workspace |
| `GET` | `/api/v1/workspaces/{id}` | Fetch one workspace |
| `DELETE` | `/api/v1/workspaces/{id}` | Delete a workspace (blocked if it still owns models) |
| `GET` | `/api/v1/models` | List models, optional `?status=` filter |
| `POST` | `/api/v1/models` | Register a model (server-generated id, validates workspace) |
| `GET` | `/api/v1/models/{modelId}/metrics` | List a model's evaluation-metric history |
| `POST` | `/api/v1/models/{modelId}/metrics` | Append a metric (updates parent's `latestAccuracy`) |

*(Endpoints from Parts 2–4 are added as the project is built.)*

**Architecture notes**
- `RestApplication` extends `javax.ws.rs.core.Application` and is annotated with
  `@ApplicationPath("/api/v1")`.
- `DataStore` is a thread-safe singleton (`ConcurrentHashMap`) shared by all resources.
- Jackson (`jersey-media-json-jackson`) performs automatic POJO ⇄ JSON conversion.
- Errors are handled centrally through dedicated `ExceptionMapper`s, so raw stack
  traces are never returned to clients.

---

## 2. How to Build & Run

**Prerequisites:** JDK 17+ (JDK 21 used here) and Apache Maven 3.8+.

```bash
# 1. Build a single runnable fat-jar
mvn clean package

# 2. Run the server
java -jar target/mlops-api.jar
```

Alternatively, run directly with Maven during development:

```bash
mvn exec:java
```

The server starts on **http://localhost:8080** and the API base path is
**http://localhost:8080/api/v1**. Press `Ctrl+C` to stop.

---

## 3. Sample `curl` Commands

```bash
# 1. Discovery endpoint – API metadata
curl http://localhost:8080/api/v1

# 2. List all workspaces
curl http://localhost:8080/api/v1/workspaces

# 3. Create a workspace (201 Created + Location header)
curl -i -X POST http://localhost:8080/api/v1/workspaces \
     -H "Content-Type: application/json" \
     -d '{"id":"WS-RL-03","teamName":"Reinforcement Learning","storageQuotaGb":250}'

# 4. Fetch a single workspace
curl http://localhost:8080/api/v1/workspaces/WS-VISION-01

# 5. Delete a workspace that still owns models -> blocked with 409 Conflict
curl -i -X DELETE http://localhost:8080/api/v1/workspaces/WS-VISION-01

# 6. Register a model (server generates the id; validates the workspace exists)
curl -i -X POST http://localhost:8080/api/v1/models \
     -H "Content-Type: application/json" \
     -d '{"framework":"PyTorch","status":"TRAINING","workspaceId":"WS-NLP-02"}'

# 7. Filter models by status
curl "http://localhost:8080/api/v1/models?status=DEPLOYED"

# 8. Register a model with a non-existent workspace -> 422 Unprocessable Entity
curl -i -X POST http://localhost:8080/api/v1/models \
     -H "Content-Type: application/json" \
     -d '{"framework":"Scikit-Learn","workspaceId":"WS-DOES-NOT-EXIST"}'

# 9. Fetch a model's evaluation-metric history (nested sub-resource)
curl http://localhost:8080/api/v1/models/MOD-8832/metrics

# 10. Append a metric -> also updates the parent model's latestAccuracy
curl -i -X POST http://localhost:8080/api/v1/models/MOD-8832/metrics \
     -H "Content-Type: application/json" \
     -d '{"accuracyScore":0.97}'

# 11. POST a metric to a DEPRECATED model -> 403 Forbidden
#     (first create one with "status":"DEPRECATED", then POST to its /metrics)
curl -i -X POST http://localhost:8080/api/v1/models/<DEPRECATED_MODEL_ID>/metrics \
     -H "Content-Type: application/json" \
     -d '{"accuracyScore":0.5}'

# 12. Trigger an unexpected runtime error -> generic 500 (no stack trace leaked)
curl -i http://localhost:8080/api/v1/_debug/trigger-error
```

---

## 4. Report — Answers to the Coursework Questions

### Part 1 – Service Architecture & Setup

**Q1.1 — When returning a Java object from a method it is automatically serialised
into JSON. Explain the role of a `MessageBodyWriter` / JSON provider (like Jackson)
in this conversion.**

JAX-RS resource methods return Java objects, but an HTTP response body is just a
stream of bytes. A `MessageBodyWriter<T>` is the JAX-RS provider interface
responsible for *marshalling* a Java type into that byte stream for a given media
type. When a method is annotated `@Produces(MediaType.APPLICATION_JSON)`, the
runtime performs **content negotiation**: it scans the registered providers and
selects one whose `isWriteable(...)` returns true for the return type and
`application/json`. Here that provider is **Jackson** (registered via
`JacksonFeature`). Jackson's `MessageBodyWriter` reflects over the object's getters,
builds the corresponding JSON structure, and writes it to the response
`OutputStream`, also setting the `Content-Type` header. The reverse direction
(JSON request body → Java object) is handled by the matching `MessageBodyReader`.
This keeps resource code free of any manual serialisation logic.

**Q1.2 — REST architecture dictates that APIs should be strictly *stateless*. Define
what statelessness means here and explain why it makes cloud APIs easier to scale
horizontally.**

Statelessness means the server keeps **no client session state between requests**:
every request must carry all the information needed to process it (credentials,
identifiers, parameters), and the server never relies on memory of a previous
request. Because no request depends on a particular server having handled an earlier
one, **any** server instance can handle **any** request. This is exactly what makes
**horizontal scaling** easy: you can place many identical instances behind a load
balancer and route each request to whichever node is free, add or remove nodes under
load, and tolerate a node failing — without sticky sessions or shared session
replication. State that must persist lives in the client (e.g. tokens) or in a shared
backing store, not in the web tier, so the application servers remain
interchangeable and trivially replaceable.

### Part 2 – Workspace Management

**Q2.1 — Discuss how implementing HTTP `Cache-Control` headers on the
`GET /workspaces` endpoint could improve performance for the client and reduce
load on the server.**

`Cache-Control` lets the server tell clients (and intermediary caches/CDNs) how
long a response may be reused. Adding e.g. `Cache-Control: max-age=60` to
`GET /workspaces` means that for the next 60 seconds the client can serve the
workspace list **straight from its local cache** without making a network call.
Benefits on both sides:

- **Client performance:** repeated reads are near-instant — no round-trip latency,
  and the UI feels faster.
- **Reduced server load:** fewer requests reach the server, so it does less work
  (no repeated lookups/serialisation) and consumes less bandwidth. This matters
  for a frequently-polled, read-heavy collection like workspaces.

Combined with **validation** caching (`ETag` + `If-None-Match`), the server can
also answer an unchanged resource with a tiny **304 Not Modified** instead of
re-sending the whole JSON body, saving bandwidth while keeping data fresh.

**Q2.2 — If a client needs to verify whether a specific workspace *exists* but
wants to save bandwidth by not downloading the JSON body, which HTTP method
should they use instead of `GET`? Explain.**

They should use **`HEAD`**. A `HEAD` request is identical to a `GET` — same URL,
same routing, same status code (`200` if it exists, `404` if not) and the same
headers (e.g. `Content-Length`) — **except the response has no body**. So the
client learns whether the workspace exists purely from the status line and
headers, without the server serialising or transmitting the full JSON payload.
This saves bandwidth and processing, which is ideal for cheap existence
("is it there?") checks. (In JAX-RS, a `@GET` method also answers `HEAD`
automatically, with the body discarded.)

### Part 3 – Model Operations & Linking

**Q3.1 — When creating a model via POST it is best practice for the *server* to
generate the unique `id` (e.g. `UUID.randomUUID()`) rather than letting the client
supply one. Discuss the security and data-integrity reasons.**

- **Data integrity / uniqueness:** the server is the single authority for the
  key space, so it can guarantee every id is unique and collision-free. If clients
  chose ids, two clients could submit the same id and overwrite each other's
  records, or accidentally clobber an existing model.
- **Security – preventing resource hijacking / IDOR:** a client-chosen id lets a
  caller *target* a specific identifier — e.g. deliberately create `MOD-8832` to
  overwrite, impersonate, or probe for another team's model. Server-generated,
  non-sequential UUIDs are effectively unguessable, so they can't be enumerated
  (`MOD-1`, `MOD-2`, …) to discover or tamper with other resources.
- **Decoupling & consistency:** the id becomes an opaque, server-controlled
  surrogate key. Clients can't encode meaning into it or depend on its format, and
  the server can store/relocate the resource freely. The authoritative id is
  returned in the `201 Created` response and `Location` header.

In this API the POST handler explicitly **ignores any `id` in the request body**
and assigns `"MOD-" + UUID.randomUUID()`.

*On validation approach:* the workspace-existence check here is done with explicit
**manual `if`-checks**, which is transparent and needs no extra dependency. An
alternative is **Jakarta Bean Validation** annotations (`@NotNull`, `@NotBlank`,
custom constraints) on the POJO, which is more declarative and centralised but
can't by itself verify cross-entity facts like "this workspace exists" — that
referential check still requires a lookup against the data store.

**Q3.2 — If a user searches for a framework containing spaces or special
characters (e.g. `?framework=Scikit Learn & Tools`), how must the client modify
the URL, and why is this encoding necessary?**

The client must **percent-encode (URL-encode)** the value, so it becomes:

```
?framework=Scikit%20Learn%20%26%20Tools
```

(`space → %20`, sometimes `+` in a query string; `& → %26`).

It's necessary because some characters are **reserved/structural** in a URL. A raw
space is not a legal URL character, and a raw `&` is the **delimiter between query
parameters** — so `?framework=Scikit Learn & Tools` would be parsed as a
`framework=Scikit Learn ` parameter plus a stray ` Tools` parameter, not as a
single value. Percent-encoding replaces each such byte with `%` followed by its
hex code, so the server receives the literal intended string and decodes it back
to `Scikit Learn & Tools` for the `@QueryParam`.

### Part 4 – Deep Nesting with Sub-Resources

**Q4.1 — You can place `@Produces(MediaType.APPLICATION_JSON)` at either the class
level or the individual method level. What is the benefit of class-level
placement, and how does method-level overriding work?**

**Class-level placement** declares a default media type for **every** method in the
resource at once. The benefit is **DRY consistency**: you write it once, every
handler inherits it, there's no risk of forgetting it on a new method, and the
intent of the whole resource ("this resource speaks JSON") is stated in one place.
`EvaluationMetricResource` uses this — both `GET` and `POST` inherit
`@Produces(APPLICATION_JSON)` from the class.

**Method-level overriding:** an annotation on a method **takes precedence over the
class-level one for that method only**. JAX-RS resolves the media type by looking
at the method first; if it has its own `@Produces`, that wins, otherwise it falls
back to the class-level value. So you set the common case once on the class and
override just the exceptional method — e.g. a class defaults to
`APPLICATION_JSON`, but one endpoint that streams a file overrides with
`@Produces(MediaType.APPLICATION_OCTET_STREAM)`. The same precedence rule applies
to `@Consumes`.

### Part 5 – Advanced Error Handling, Exception Mapping & Logging

**Q5.2 — Explain fundamentally why a validation failure caused by the user
providing a non-existent `workspaceId` must return a 4xx code rather than a 5xx
code.**

HTTP status classes assign **responsibility for the error**. The **4xx** class
means *"the client made a mistake"* — the request itself is faulty and the server
correctly refused it. The **5xx** class means *"the server failed"* — the request
was reasonable but the server broke while handling it. A non-existent
`workspaceId` is a defect in the **client's input**: the server worked perfectly,
detected the bad reference, and rejected it on purpose. So it must be a 4xx (here
**422 Unprocessable Entity**: the JSON was syntactically valid but semantically
invalid). Returning 5xx would falsely blame the server, mislead monitoring/alerts,
and wrongly signal to the client that **retrying the identical request** might
succeed — whereas a 4xx correctly tells the client to **fix the request** before
retrying.

**Q5.4 — If an operation throws a specific custom exception (e.g.
`LinkedWorkspaceNotFoundException`) and you also have a global
`ExceptionMapper<Throwable>`, how does the JAX-RS runtime decide which mapper to
run?**

JAX-RS picks the **most specific** applicable mapper, by walking up the thrown
exception's type hierarchy and choosing the mapper whose generic type is the
**nearest superclass** of the exception. `LinkedWorkspaceNotFoundException` has a
dedicated `ExceptionMapper<LinkedWorkspaceNotFoundException>`, which is an exact
(closest) match, so it wins. The `ExceptionMapper<Throwable>` only matches via the
most distant ancestor (`Throwable`), so it is selected **only when no more
specific mapper exists** — making it a true fallback safety net. (In this project
the global mapper additionally re-emits the embedded response of any
`WebApplicationException`, so deliberate errors like a `NotFoundException`'s 404
keep their status instead of becoming a 500.)

**Q5.5 — In your filter you interact with `ContainerRequestContext` and
`ContainerResponseContext`. List two pieces of crucial HTTP metadata you can
extract from these contexts that are highly valuable for debugging server
issues.**

1. **The request line — HTTP method + full request URI** (from
   `ContainerRequestContext.getMethod()` and `getUriInfo().getRequestUri()`):
   tells you *exactly what was called*, including path and query parameters — the
   first thing you need to reproduce or locate a problem.
2. **The response status code** (from `ContainerResponseContext.getStatus()`):
   tells you the *outcome* of the call (2xx success vs 4xx/5xx failure), so you can
   immediately spot failing endpoints in the logs.

Other highly useful items available from these contexts include the **headers**
(`getHeaders()` — e.g. `Content-Type`, `Authorization`, correlation/trace ids) and
the **media type**, which help diagnose content-negotiation and auth problems.

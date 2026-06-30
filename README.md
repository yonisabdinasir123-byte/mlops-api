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

When my resource methods return a Java object, JAX-RS doesn't actually send the
object itself, because an HTTP response body is just a stream of bytes. Something
has to turn the object into those bytes, and that is the job of a
`MessageBodyWriter`. It is the provider interface JAX-RS uses to serialise a given
Java type into a given media type.

Because I annotated my methods with `@Produces(MediaType.APPLICATION_JSON)`, the
runtime looks through the providers it knows about and picks one that can write my
return type as `application/json` (it checks each provider's `isWriteable(...)`
method). In this project that provider is Jackson, which I enabled by registering
`JacksonFeature`. Jackson reads the object's getters, builds the matching JSON,
writes it to the response output stream and sets the `Content-Type` header for me.
The opposite direction, taking a JSON request body and turning it back into a Java
object, is handled by the equivalent `MessageBodyReader`. The point is that I never
have to write any parsing or string-building code myself; the provider does all of
it.

**Q1.2 — REST architecture dictates that APIs should be strictly *stateless*. Define
what statelessness means here and explain why it makes cloud APIs easier to scale
horizontally.**

Stateless means the server doesn't remember anything about a client between
requests; it holds no session in memory. Every request has to bring everything the
server needs to deal with it, so things like the resource id, any parameters and
the authentication all travel with each request rather than being left on the
server from a previous call.

This is what makes horizontal scaling straightforward. If no request depends on a
particular server having handled an earlier one, then it doesn't matter which
server picks it up. I can run lots of identical instances behind a load balancer
and send each request to whichever one is free, spin up more instances when traffic
grows, and lose an instance without breaking anyone's "session", because there
isn't one to lose. Anything that genuinely has to persist lives either on the
client (for example a token) or in a shared store that every instance can reach,
not inside the web server itself. That is what keeps the instances interchangeable,
which is the whole reason you can scale out by just adding more of them.

### Part 2 – Workspace Management

**Q2.1 — Discuss how implementing HTTP `Cache-Control` headers on the
`GET /workspaces` endpoint could improve performance for the client and reduce
load on the server.**

`Cache-Control` is how the server tells the client (and any caches or CDNs in
between) whether a response can be reused and for how long. If I add something like
`Cache-Control: max-age=60` to `GET /workspaces`, then for the next minute the
client can just reuse the copy it already has instead of asking the server again.

For the client that means the data is effectively instant on repeat reads, because
there is no network round-trip. For the server it means fewer requests actually
reach it, so it isn't repeatedly looking up the same list and serialising it to
JSON, and it sends less data over the wire. That is a real win for a list endpoint
like workspaces, which clients are likely to read often and which doesn't change
every second.

I could take it a step further with `ETag` and `If-None-Match` (validation
caching): the client sends back the tag it already has, and if nothing has changed
the server can reply `304 Not Modified` with no body at all, so the data stays
fresh but I still avoid resending the whole payload.

**Q2.2 — If a client needs to verify whether a specific workspace *exists* but
wants to save bandwidth by not downloading the JSON body, which HTTP method
should they use instead of `GET`? Explain.**

They should use `HEAD`. A `HEAD` request behaves exactly like a `GET`: same URL,
same routing, and it comes back with the same status code (`200` if the workspace
is there, `404` if it isn't) and the same headers. The only difference is that the
response has no body. So the client can tell whether the workspace exists just from
the status code, without the server having to build and send the full JSON. That is
ideal when you only care about existence and don't want to waste bandwidth pulling
down data you're only going to throw away. A nice detail in JAX-RS is that I don't
have to write a separate handler for this, because a `@GET` method automatically
answers `HEAD` as well, just with the body left off.

### Part 3 – Model Operations & Linking

**Q3.1 — When creating a model via POST it is best practice for the *server* to
generate the unique `id` (e.g. `UUID.randomUUID()`) rather than letting the client
supply one. Discuss the security and data-integrity reasons.**

Letting the server generate the id (I use `UUID.randomUUID()`) is better for a few
reasons.

The main one is integrity. If the server owns the id, it is the single source of
truth for the key space and can guarantee the ids are unique. If clients picked
their own, two of them could end up choosing the same id, or a client could pick an
id that already exists and overwrite another model's record by accident.

There is also a security side to it. If a client can choose the id, it can
deliberately target one, for example creating or overwriting `MOD-8832` to clobber
or impersonate another team's model. And if the ids were predictable or sequential
(`MOD-1`, `MOD-2`, and so on) an attacker could simply count upwards to find or
poke at resources that aren't theirs, which is the classic insecure-direct-object-
reference (IDOR) problem. A random UUID isn't guessable, so that approach doesn't
get them anywhere.

Finally it keeps things clean: the id is an opaque value that the client shouldn't
read meaning into or depend on, and the server hands the real id back in the
`201 Created` response and the `Location` header. In my POST handler I deliberately
ignore any `id` sent in the body and set it to `"MOD-" + UUID.randomUUID()`.

It is also worth contrasting how the validation itself is done. I check that the
workspace exists with a plain `if` statement, which is easy to follow and needs no
extra libraries. The alternative is Jakarta Bean Validation annotations such as
`@NotNull` and `@NotBlank` on the model, which is tidier for simple field rules,
but it can't on its own check something like "this workspace actually exists",
since that is a cross-entity fact that still needs a lookup in the data store.

**Q3.2 — If a user searches for a framework containing spaces or special
characters (e.g. `?framework=Scikit Learn & Tools`), how must the client modify
the URL, and why is this encoding necessary?**

The client has to percent-encode (URL-encode) the value, so
`?framework=Scikit Learn & Tools` becomes:

```
?framework=Scikit%20Learn%20%26%20Tools
```

(the space becomes `%20`, or sometimes `+` in a query string, and the `&` becomes
`%26`).

This is needed because some characters have a special meaning in a URL. A space
isn't actually a legal URL character at all, and `&` is the separator between query
parameters. So if the raw string went through unencoded, the server would read it
as a parameter `framework=Scikit Learn` followed by a separate, meaningless `Tools`
parameter, instead of as one value. Percent-encoding swaps each of those characters
for a `%` and its hex code, so the value travels across safely and the server
decodes it back to the original `Scikit Learn & Tools` when it binds it to my
`@QueryParam`.

### Part 4 – Deep Nesting with Sub-Resources

**Q4.1 — You can place `@Produces(MediaType.APPLICATION_JSON)` at either the class
level or the individual method level. What is the benefit of class-level
placement, and how does method-level overriding work?**

Putting `@Produces` on the class sets a default media type for every method in that
resource in one go. The benefit is that I write it once and every handler inherits
it, so I can't forget it on a new method, and the whole resource clearly "speaks
JSON" from a single place. My `EvaluationMetricResource` does exactly this: both the
`GET` and the `POST` inherit `@Produces(APPLICATION_JSON)` from the class.

Method-level overriding works because an annotation on a method takes priority over
the class-level one, but only for that method. JAX-RS looks at the method first: if
it has its own `@Produces`, that is used; if not, it falls back to the class
default. So the usual pattern is to set the common case on the class and only
override the odd method that needs to be different, for example a class that is JSON
by default but one endpoint that returns a file with
`@Produces(MediaType.APPLICATION_OCTET_STREAM)`. `@Consumes` follows the same rule.

### Part 5 – Advanced Error Handling, Exception Mapping & Logging

**Q5.2 — Explain fundamentally why a validation failure caused by the user
providing a non-existent `workspaceId` must return a 4xx code rather than a 5xx
code.**

The HTTP status classes are basically about whose fault the error is. A 4xx says
the client got something wrong and the server correctly refused it, whereas a 5xx
says the request was fine but the server itself broke while handling it.

A `workspaceId` that doesn't exist is a problem with what the client sent, not a
server failure. The server did its job, spotted that the reference was invalid and
rejected it on purpose, so it has to be a 4xx. I return `422 Unprocessable Entity`
specifically, because the JSON was well-formed but semantically wrong (it points at
something that isn't there). Returning a 5xx would be misleading on a few levels: it
would blame the server, it would make monitoring and alerting think the service is
failing, and it would suggest to the client that the same request might work if it
just retries, when really the client needs to change the request first.

**Q5.4 — If an operation throws a specific custom exception (e.g.
`LinkedWorkspaceNotFoundException`) and you also have a global
`ExceptionMapper<Throwable>`, how does the JAX-RS runtime decide which mapper to
run?**

JAX-RS chooses the most specific mapper for the exception that was actually thrown.
It looks at the exception's type and walks up its class hierarchy, then picks the
`ExceptionMapper` whose type parameter is the closest match.
`LinkedWorkspaceNotFoundException` has its own
`ExceptionMapper<LinkedWorkspaceNotFoundException>`, which is an exact match, so
that one runs. The `ExceptionMapper<Throwable>` only matches through the most
distant ancestor, `Throwable`, so it is effectively the last resort and only kicks
in when there is nothing more specific, which is exactly what I want from a global
catch-all. One extra thing in my version: the global mapper first checks whether the
exception is a `WebApplicationException` and, if it is, returns the response already
attached to it. That way a deliberate error such as a `NotFoundException` keeps its
404 instead of being flattened into a 500.

**Q5.5 — In your filter you interact with `ContainerRequestContext` and
`ContainerResponseContext`. List two pieces of crucial HTTP metadata you can
extract from these contexts that are highly valuable for debugging server
issues.**

Two of the most useful pieces are the HTTP method together with the request URI,
and the response status code.

I get the method and URI from the request context (`getMethod()` and
`getUriInfo().getRequestUri()`). Together these tell me exactly what was called,
including the path and any query parameters, which is the first thing I need in
order to reproduce or track down a problem. The response status code comes from the
response context (`getStatus()`), and that tells me how the call actually ended,
whether it succeeded or came back as a 4xx or 5xx, so I can quickly pick the failing
requests out of the log.

Beyond those two, the headers (`getHeaders()`, for example `Content-Type`,
`Authorization`, or a correlation id) are also valuable, since a lot of bugs come
down to the wrong content type or a missing or invalid auth header.

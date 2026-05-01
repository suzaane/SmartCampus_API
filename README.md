# Smart Campus Sensor & Room Management API

**Student:** Suzane Shah  
**Student ID:** w2055187  
**Module:** 5COSC022W Client-Server Architectures  
**University:** University of Westminster

---

## API Overview

This is a RESTful API for managing campus rooms and IoT sensors, built with JAX-RS(Jersey) running on an embedded Grizzly HTTP server. No external server installation is required — the server starts from a single Java main method.

The system manages three core resources:

- **Rooms** — physical campus spaces with capacity limits
- **Sensors** — IoT devices assigned to rooms (temperature, CO2, occupancy)
- **Sensor Readings** — historical measurement records per sensor

### Architectural decisions

**JAX-RS with Jersey** was chosen as the REST framework because it is the reference implementation of the JAX-RS specification and integrates cleanly with Jackson for JSON serialisation.

**Embedded Grizzly server** removes the need for a separate servlet container like Tomcat. The server boots inside `Main.java` and is ready immediately.

**Sub-resource locator pattern** is used for sensor readings. `SensorResource` delegates `/sensors/{id}/readings` to a dedicated `SensorReadingResource` class rather than handling all paths in one file. This keeps each class focused and mirrors the URL hierarchy in the code structure.

**Centralised DataStore** holds all in-memory data using `ConcurrentHashMap` for rooms and sensors and `CopyOnWriteArrayList` for reading histories. This is necessary because JAX-RS creates a new resource class instance per request — any data stored inside a resource class field disappears when the request ends.

**Exception mappers** catch specific exceptions and return clean JSON error responses. No raw Java stack traces are ever exposed to the client.

**ContainerRequestFilter and ContainerResponseFilter** log every request method, URI, and response status code from a single centralised filter class.

---

## How to run
### Option 1: Run from NetBeans (Recommended)
1. Open the project in NetBeans
2. Locate `Main.java` (com.smartcampus.Main)
3. Right-click → Run File (or Run Project)
4. Wait for console message:

   Server started at : "http://localhost:8080/api/v1/"


## Project structure
```
src/main/java/com/smartcampus/
├── Main.java                          
├── ApplicationConfig.java             
├── model/
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── resource/
│   ├── DiscoveryResource.java         
│   ├── RoomResource.java              
│   ├── SensorResource.java            
│   └── SensorReadingResource.java     
├── exception/
│   ├── RoomNotEmptyException.java     
│   ├── LinkedResourceNotFoundException.java
│   ├── SensorUnavailableException.java       
│   └── mapper/
│       ├── RoomNotEmptyExceptionMapper.java
│       ├── LinkedResourceNotFoundExceptionMapper.java
│       ├── SensorUnavailableExceptionMapper.java
│       └── GlobalExceptionMapper.java       
├── filter/
│   └── LoggingFilter.java             
└── service/
    └── DataStore.java

```

## Seed Data

The following data is loaded automatically on startup. No setup required.

| Resource | ID       | Details                              |
|----------|----------|--------------------------------------|
| Room     | LIB-301  | Library Quiet Study, capacity 50     |
| Room     | ENG-101  | Engineering Lab A, capacity 25       |
| Sensor   | TEMP-001 | Temperature, ACTIVE, in LIB-301      |
| Sensor   | CO2-001  | CO2, ACTIVE, in LIB-301              |
| Sensor   | OCC-001  | Occupancy, MAINTENANCE, in ENG-101   |

---

## API Endpoints

| Method | Path | Description | Success Code |
|--------|------|-------------|--------------|
| GET    | /api/v1/ | Discovery + HATEOAS links | 200 |
| GET    | /api/v1/rooms | List all rooms | 200 |
| POST   | /api/v1/rooms | Create a room | 201 |
| GET    | /api/v1/rooms/{roomId} | Get room by ID | 200 |
| DELETE | /api/v1/rooms/{roomId} | Delete room (blocked if has sensors) | 204 |
| GET    | /api/v1/sensors | List all sensors | 200 |
| GET    | /api/v1/sensors?type=CO2 | Filter sensors by type | 200 |
| GET    | /api/v1/sensors/{sensorId} | Get sensor by ID | 200 |
| POST   | /api/v1/sensors | Create sensor (validates roomId) | 201 |
| GET    | /api/v1/sensors/{sensorId}/readings | Get reading history | 200 |
| POST   | /api/v1/sensors/{sensorId}/readings | Add a new reading | 201 |

---

## Error Responses

All errors return a JSON body. No stack traces are ever exposed.

| Code | Scenario |
|------|----------|
| 400  | Missing required field in request body |
| 403  | Posting a reading to a MAINTENANCE sensor |
| 404  | Resource not found |
| 409  | Attempting to delete a room that still has sensors |
| 415  | Request sent with wrong Content-Type (not application/json) |
| 422  | Creating a sensor with a roomId that does not exist |
| 500  | Unexpected server error — generic message returned |


---

## Sample curl Commands

```bash
# 1. Discovery endpoint — returns version, contact, and resource links
curl http://localhost:8080/api/v1/

# 2. List all rooms
curl http://localhost:8080/api/v1/rooms

# 3. Create a new room
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-302","name":"Library Group Study","capacity":30}'

# 4. Get a specific room by ID
curl http://localhost:8080/api/v1/rooms/LIB-302

# 5. Delete a room with no sensors (success — 204)
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-302

# 6. Attempt to delete a room that has sensors (fails — 409 Conflict)
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301

# 7. Create a sensor linked to an existing room
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":22.5,"roomId":"LIB-301"}'

# 8. List all sensors
curl http://localhost:8080/api/v1/sensors

# 9. Filter sensors by type
curl "http://localhost:8080/api/v1/sensors?type=Temperature"

# 10. Get a specific sensor by ID
curl http://localhost:8080/api/v1/sensors/TEMP-001

# 11. Add a reading to a sensor (also updates currentValue on the sensor)
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.5}'

# 12. Get all readings for a sensor
curl http://localhost:8080/api/v1/sensors/TEMP-001/readings

# 13. Attempt to post a reading to a MAINTENANCE sensor (fails — 403)
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":5.0}'

# 14. Attempt to create a sensor with a non-existent roomId (fails — 422)
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"BAD-001","type":"CO2","status":"ACTIVE","roomId":"FAKE-999"}'

# 15. Send wrong Content-Type to a POST endpoint (fails — 415)
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: text/plain" \
  -d 'this is not json'
```

## Report — Question Answers

---

### Part 1: Service Architecture & Setup

#### 1.1 Project & Application Configuration

**Default lifecycle of a JAX-RS resource class and its impact on in-memory data management**

The application uses a class called `ApplicationConfig` that extends `javax.ws.rs.core.Application`. This class is annotated with `@ApplicationPat ("/api/v1")`, which registers the base URI for all API resources. Without this class, Jersey would not know where to route incoming HTTP requests.

In a standard JAX-RS implementation such as Jersey, resource classes are request-scoped by default. This means every time an HTTP request arrives, the framework creates a brand new instance of the resource class, for example `RoomResource` or `SensorResource`. Once the request finishes, that instance is thrown away. A resource class is only treated as a singleton if it is explicitly annotated with `@Singleton`.

Because a new object is created per request, any data stored inside resource class fields will not survive between requests — it disappears the moment the request ends. This means resource classes must never hold mutable state.

To solve this, the application uses a shared class called `DataStore`. This class holds all data in memory and is accessible across every request. Thread-safe collections are used to handle concurrent access safely:

- `ConcurrentHashMap` stores rooms and sensors. It allows safe `put()` and `get()` operations from multiple threads simultaneously without requiring external locks.
- `CopyOnWriteArrayList` stores sensor reading histories. It allows safe iteration
  even when other threads are adding new items at the same time.

This design ensures data persists and stays consistent across all requests, even though each request gets a fresh resource instance.

---

#### 1.2 The "Discovery" Endpoint

**Why HATEOAS is a hallmark of advanced RESTful design and its benefits over static documentation**

HATEOAS stands for Hypermedia as the Engine of Application State. It means the server includes links to available actions directly inside its responses, so clients do not need to manually construct URLs or rely on external documentation to know
what they can do next. This is considered advanced REST design because it removes tight coupling between the client and the server.

**Runtime discoverability** — A client can start at `GET /api/v1` and immediately discover available resources such as `"rooms": "/api/v1/rooms"` directly from the response. No documentation is needed to get started.

**Reduced coupling** — If the server changes its URL structure, clients that follow the links in responses continue to work. Clients that hard-code URLs break immediately.

**Self-descriptive responses** — Responses describe what actions are available, making the API understandable without reading a separate document.

**Easier evolution** — New resources can be added to the response without breaking existing clients. Clients simply ignore links they do not recognise.

The `DiscoveryResource` in this project follows Level 3 of the Richardson Maturity Model by returning a links map and an actions map, allowing clients to navigate the API dynamically from a single entry point.

---

### Part 2: Room Management

#### 2.1 Room Resource Implementation

**Implications of returning only IDs versus returning full room objects**

When designing `GET /api/v1/rooms`, the API can return either just room IDs or complete room objects. Each choice has real trade-offs.

If only IDs are returned, the payload is very small and uses minimal bandwidth. However, the client must then make a separate `GET /api/v1/rooms/{id}` request for every room it wants details about. This is the N+1 request problem. If there are 50 rooms, the client makes 51 requests instead of one, which increases total latency significantly.

If full objects are returned, the response is larger because it includes name, capacity, and sensor ID lists for every room. However, the client gets everything it needs in a single request with no follow-up calls required.

Returning only IDs makes sense for lightweight use cases like dropdown menus or search autocomplete where only identifiers are needed. Returning full objects is better for management dashboards where complete information is needed immediately.

This API returns full room objects because campus management systems typically need room names, capacities, and sensor assignments all at once. In larger systems, a `?fields=id,name` parameter could be added to let clients request only the fields they need, reducing payload size without the N+1 problem.

---

#### 2.2 Room Deletion & Safety Logic

**Is the DELETE operation idempotent in this implementation?**

Yes, `DELETE /api/v1/rooms/{roomId}` is idempotent in this implementation.

Idempotency means that sending the same request multiple times produces the same final state on the server. According to RFC 7231, idempotency is defined by the effect on server state, not by the response code returned.

**Scenario 1 — Room exists with no sensors:** The first `DELETE` removes the room and returns `204 No Content`. Sending the same request again returns `404 Not Found`. The response code differs but the server state is identical — the room is gone and has not been changed by the second request.

**Scenario 2 — Room exists with sensors:** The `DELETE` returns `409 Conflict` and the room is not removed. Sending the same request again still returns `409 Conflict` and the room remains unchanged. No matter how many times the request is repeated, the server state does not change.

**Scenario 3 — Room never existed:** The request returns `404 Not Found` immediately. Repeating it produces the same result.

In all three scenarios, repeated requests do not alter server state after the first attempt. The operation is therefore idempotent.

---

### Part 3: Sensor Operations & Linking

#### 3.1 Sensor Resource & Integrity

**Consequences of sending a non-JSON payload to a `@Consumes(MediaType.APPLICATION_JSON)` endpoint**

If a client sends a request with `Content-Type: text/plain` or any format other than `application/json`, JAX-RS rejects the request before it ever reaches the `createSensor` method.

The server returns `HTTP 415 Unsupported Media Type`.

This happens because Jersey uses `MessageBodyReader` providers to deserialise incoming request bodies into Java objects. When a request arrives, Jersey searches for a reader that can handle both the request's media type and the target Java class (`Sensor`). The `JacksonFeature` registered in `ApplicationConfig` provides a reader for `application/json` only. If the incoming request is not JSON, no reader matches and Jersey immediately returns a 415 response without invoking any business logic.

This behaviour enforces a strict contract. Clients must send JSON. The `@Consumes(MediaType.APPLICATION_JSON)` annotation documents what the endpoint accepts, and `@Produces(MediaType.APPLICATION_JSON)` ensures responses are always returned as JSON. This makes the API predictable and consistent.

---

#### 3.2 Filtered Retrieval & Search

**Why `@QueryParam` is superior to embedding the filter in the URL path**

This API uses `@QueryParam("type")` to filter sensors:
GET /api/v1/sensors?type=CO2

An alternative design would embed the filter in the path:
GET /api/v1/sensors/type/CO2

Query parameters are the correct choice for filtering for several reasons.

Path segments identify a specific resource. Query parameters filter or modify how a collection is returned. Using a path segment for filtering implies that `/sensors/type/CO2` is a different resource from `/sensors`, which is architecturally incorrect — they are the same collection, just filtered differently.

Query parameters are naturally optional. Without `?type=CO2`, the endpoint returns all sensors. The same endpoint handles both cases without needing two separate paths.

Multiple filters combine cleanly:
GET /api/v1/sensors?type=CO2&status=ACTIVE

An alternative design would embed the filter in the path:
GET /api/v1/sensors/type/CO2

Query parameters are the correct choice for filtering for several reasons.

Path segments identify a specific resource. Query parameters filter or modify how a collection is returned. Using a path segment for filtering implies that `/sensors/type/CO2` is a different resource from `/sensors`, which is architecturally incorrect — they are the same collection, just filtered differently.

Query parameters are naturally optional. Without `?type=CO2`, the endpoint returns all sensors. The same endpoint handles both cases without needing two separate paths.

Multiple filters combine cleanly:
GET /api/v1/sensors?type=CO2&status=ACTIVE

Embedding multiple filters in path segments produces ugly, rigid URLs that are
difficult to extend.

If the `type` query parameter is provided but no sensors match, the API returns an empty JSON array with `200 OK`. It does not return `404 Not Found` because the `/sensors` collection itself exists — it simply has no matching results.

REST best practice is to use path segments to identify resources and query parameters to filter, search, or sort them.

---

### Part 4: Deep Nesting with Sub-Resources

#### 4.1 The Sub-Resource Locator Pattern

**Architectural benefits and how JAX-RS resolves the locator at runtime**

The sub-resource locator pattern is implemented using a method that carries a `@Path` annotation but has no HTTP verb annotation such as `@GET` or `@POST`:

```java
@Path("/{sensorId}/readings")
public SensorReadingResource getSensorReadingResource(
    @PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
}
```

Because this method has no HTTP verb, JAX-RS does not treat it as an endpoint. Instead, when a request arrives at `/sensors/TEMP-001/readings`, JAX-RS calls this method to get a resource object, then continues matching the remaining path againstthe annotations inside the returned `SensorReadingResource` class. The first classhands off to the second class — this is the locator pattern.

This design provides several architectural benefits.

**Separation of concerns** — `SensorResource` handles sensor-level operations. `SensorReadingResource` handles reading history. Neither class knows or cares about the other's internal logic.

**Context encapsulation** — The `sensorId` is passed into `SensorReadingResource` when it is constructed. All methods inside that class use it directly without extracting path parameters again.

**Avoids the god class** — Without this pattern, all endpoints would live inside one massive class that becomes impossible to maintain as the API grows.

**Mirrors the API structure** — The code hierarchy `SensorResource → SensorReadingResource` directly reflects the URL hierarchy `/sensors → /sensors/{id}/readings`. This makes the codebase easy to navigate.

**Easier to test** — Each resource class can be unit tested independently.

---

#### 4.2 Historical Data Management

**Implementation and side effect consistency**

`SensorReadingResource` provides two endpoints:

- `GET /` — returns all historical readings for a specific sensor.
- `POST /` — appends a new reading for that sensor.

Before adding a reading, the API verifies that the sensor exists. If the sensor is in `MAINTENANCE` status, the API returns `403 Forbidden` — readings cannot be recorded for sensors that are not operational.

When a `POST` is successful, the system immediately updates the `currentValue` field on the parent `Sensor` object:

```java
sensor.setCurrentValue(reading.getValue());
```

This ensures the sensor always reflects the most recent measurement. A client calling `GET /api/v1/sensors/TEMP-001` after posting a reading will see the updated `currentValue` without any additional steps.

If no ID is provided in the request body, the API generates a UUID automatically. If no timestamp is provided, the API assigns the current epoch time in milliseconds. This reduces unnecessary work on the client side and ensures every reading has a valid, unique identity.

---

### Part 5: Advanced Error Handling, Exception Mapping & Logging

#### 5.1 Resource Conflict (409 Conflict)

**Why 409 is returned when deleting a room that still contains sensors**

When a client sends `DELETE /api/v1/rooms/LIB-301` and that room still has sensors assigned to it, the API returns `HTTP 409 Conflict`.

`409` is used here instead of `400 Bad Request` because the request itself is not malformed. The URL is correct, the method is correct, and the format is correct. The problem is that the server cannot carry out the deletion because of a business rule — a room with active sensors cannot be removed, as it would leave those sensors without a valid room reference and create orphaned data in the system.

`409 Conflict` specifically means the request conflicts with the current state of the resource on the server. That is exactly the situation here. The custom error response returned is:

```json
{
  "error": "RoomNotEmpty",
  "message": "Cannot delete room. Remove all sensors first."
}
```

No stack trace is exposed. The message is clear, actionable, and tells the client exactly what needs to be done before the deletion can proceed.

---

#### 5.2 The Global Safety Net (500)

**Cybersecurity risks of exposing internal Java stack traces**

Exposing raw stack traces in API responses creates serious security vulnerabilities.

Stack traces reveal the technology stack — Java version, framework names like Jersey, internal package names and class names. Attackers use this to identify known CVEs and exploit them.

They expose internal file paths and source code locations, which helps attackers map the structure of the application and identify attack surfaces.

They can reveal business logic — how validation works, what conditions cause failures, what data structures are used. This helps attackers craft targeted requests designed to bypass security checks.

In some cases stack traces print variable values, which can include sensitive data that should never leave the server.

To prevent all of this, `GlobalExceptionMapper` catches every unhandled exception and returns only this:

```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please contact support."
}
```

The full stack trace is written to the server log using `java.util.logging.Logger` so developers can debug internally. The client sees nothing useful to an attacker. This follows OWASP recommendations: show generic error messages externally, log full detail internally.

---

#### 5.3 API Request & Response Logging Filters

**Why JAX-RS filters are better than manual logging inside resource methods**

Using `ContainerRequestFilter` and `ContainerResponseFilter` for logging is significantly better than adding log statements inside individual resource methods.

**Separation of concerns** — Logging is infrastructure, not business logic. Resource classes should focus entirely on handling requests. Mixing logging code into resource methods makes them harder to read and maintain.

**DRY principle** — One filter covers every endpoint automatically. Without filters, every single method in every resource class needs its own logging code. Miss one and that endpoint has no observability.

**Guaranteed coverage** — Filters run on every request and response without exception. Manual logging can be forgotten or skipped. Filters cannot.

**Access to complete metadata** — Filters have access to the full request URI, HTTP method, all headers, and the final response status code.

**Performance monitoring** — A filter records the time a request arrives and the time the response is sent. The difference is the processing time. This is not possible to do cleanly inside a resource method.

**Chainable and conditional** — Multiple filters can be stacked. A logging filter, an authentication filter, and a rate-limiting filter can all run in sequence without knowing about each other.

In this project, `LoggingFilter` records the HTTP method, request URI, and response status code for every API call:

POST /api/v1/sensors
<<< 201

This gives complete visibility into API traffic from a single, centralised location.
```


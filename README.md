# Smart Campus RESTful API — JAX-RS / Jersey / Grizzly

## Overview
A RESTful API for managing campus rooms and IoT sensors, built with JAX-RS running on an embedded Grizzly HTTP server.

The system manages:
- Campus rooms
- IoT sensors assigned to rooms
- Historical sensor readings

The API follows REST best practices including:

- HATEOAS discovery endpoint
- Proper HTTP status codes
- Exception mapping
- Request/response logging filters
- Sub-resource locators for nested resources

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


---

# Seed Data (Available Immediately on Startup)

| Resource | ID | Details |
|--------|------|---------|
| Room | LIB-301 | Library Quiet Study, capacity 50 |
| Room | ENG-101 | Engineering Lab A, capacity 25 |
| Sensor | TEMP-001 | Temperature, ACTIVE, in LIB-301 |
| Sensor | CO2-001 | CO2, ACTIVE, in LIB-301 |
| Sensor | OCC-001 | Occupancy, MAINTENANCE, in ENG-101 |

---

# API Endpoints

| Method | Path | Description | Success |
|------|------|------|------|
| GET | /api/v1/ | Discovery + HATEOAS links | 200 |
| GET | /api/v1/rooms | List all rooms | 200 |
| POST | /api/v1/rooms | Create a room | 201 |
| GET | /api/v1/rooms/{roomId} | Get room by ID | 200 |
| DELETE | /api/v1/rooms/{roomId} | Delete room (blocked if has sensors) | 204 |
| GET | /api/v1/sensors | List all sensors | 200 |
| GET | /api/v1/sensors?type=CO2 | Filter sensors by type | 200 |
| GET | /api/v1/sensors/{sensorId} | Get sensor by ID | 200 |
| POST | /api/v1/sensors | Create sensor (validates roomId) | 201 |
| GET | /api/v1/sensors/{sensorId}/readings | Get reading history | 200 |
| POST | /api/v1/sensors/{sensorId}/readings | Add reading | 201 |

---

# Error Responses

| Code | When |
|---|---|
| 400 | Missing required field |
| 403 | POST reading to MAINTENANCE sensor |
| 404 | Resource not found |
| 409 | DELETE room with sensors |
| 415 | Unsupported media type |
| 422 | Sensor created with non-existent roomId |
| 500 | Unexpected server error |

```

```
## Example curl commands
```bash
# Discovery
curl http://localhost:8080/api/v1/

# List all rooms
curl http://localhost:8080/api/v1/rooms

# Create a room
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-302","name":"Library Quiet Study","capacity":50}'

# Create a room by id
curl http://localhost:8080/api/v1/rooms/LIB-302


# Create a room by id
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-302

# Create a sensor (roomId must exist)
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":22.5,"roomId":"LIB-301"}'

#List sensor
curl http://localhost:8080/api/v1/sensors

# Filter sensors by type
curl "http://localhost:8080/api/v1/sensors?type=Temperature"

# Add a reading (updates currentValue on the sensor)
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.5}'

# Get all readings for a sensor
curl http://localhost:8080/api/v1/sensors/TEMP-001/readings

# Delete a room that has no sensors (success: 204)
curl -X DELETE http://localhost:8080/api/v1/rooms/CS-101

# Delete a room that HAS sensors (fails: 409)
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301

# POST to MAINTENANCE sensor (fails: 403)
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":5.0}'

# POST sensor with invalid roomId (fails: 422)
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"BAD-001","type":"CO2","status":"ACTIVE","roomId":"FAKE-999"}'
```

#Question and answers

# Part 1: Service Architecture & Setup

## 1.1 Project & Application Configuration
Default lifecycle of a JAX RS resource class and its impact on in memory data management
In a standard JAX-RS implementation such as Jersey, resource classes are request-scoped by default. This means that every time an HTTP request arrives, the framework creates a new instance of the resource class (for example RoomResource or SensorResource). After the request is completed, that instance is discarded. The resource is only treated as a singleton if it is explicitly annotated with @Singleton.
Because a new object is created for every request, data stored inside resource class fields will not persist between requests. Any data saved there would disappear once the request finishes. Therefore, resource classes should not store mutable state.
To solve this, the application uses a shared storage layer called DataStore. This class stores the data in memory and is accessible by all requests.
Thread-safe collections are used to safely manage concurrent access:
•	ConcurrentHashMap is used to store sensors and rooms. It allows safe put() and get() operations without requiring external locks. 
•	CopyOnWriteArrayList is used for sensor lists and reading histories. This structure allows safe iteration even when multiple requests modify the list at the same time. 
This design ensures that data remains consistent and accessible across all requests, even though each request uses a new resource instance.


## 1.2 The “Discovery” Endpoint
Why HATEOAS is a hallmark of advanced RESTful design and its benefits over static documentation
HATEOAS (Hypermedia as the Engine of Application State) is an important principle of REST. It means that the server guides the client by providing links to available actions inside the response.
Instead of clients manually constructing URLs, they simply follow the links provided by the API.
This approach is considered an advanced REST design because it reduces the dependency between the client and server.
Compared to static documentation, HATEOAS has several benefits:
Runtime discoverability
A client can start from the root endpoint (GET /api/v1/) and discover available resources like "rooms": "/api/v1/rooms" directly from the response.
Reduced coupling
If the server changes its URL structure (for example /api/v1/rooms to /api/v2/spaces/rooms), clients will still work because they follow the provided links instead of hard-coding URLs.
Self-descriptive responses
The API responses include information about available actions, making the system easier to understand and use.
Easier evolution
New resources or relationships can be added without breaking existing clients, because clients ignore links they do not recognize.
The DiscoveryResource in this project follows Level 3 of the Richardson Maturity Model by returning both a links map and an actions map, allowing clients to discover and interact with the API dynamically.


# Part 2: Room Management

## 2.1 Room Resource Implementation
Implications of returning only IDs versus full room objects
When designing the GET /rooms endpoint, the API can return either only room IDs or full room objects. Each approach has advantages and disadvantages.
If only IDs are returned, the response is very small and uses less network bandwidth. However, the client must make additional requests (GET /rooms/{id}) to get the full details. This can create the N+1 request problem, increasing total latency.
If full objects are returned, the response size is larger because it includes all room information such as name, capacity, and sensor IDs. However, the client receives everything in a single request, which reduces the need for additional API calls.
Returning only IDs is useful for scenarios like dropdown lists or autocomplete search, where only identifiers are required.
Returning full objects is better for dashboards or management systems, where the client needs complete information immediately.
In this project, the API returns full room objects because campus management systems usually need room names, capacities, and sensors directly.
In larger systems, field filtering (for example ?fields=id,name) could be used to reduce payload size when necessary.


## 2.2 Room Deletion & Safety Logic
Idempotency of the DELETE operation
Yes, the DELETE /rooms/{roomId} operation is idempotent in this implementation.
Idempotency means that sending the same request multiple times produces the same final result on the server.
If the room exists and has no sensors, the first DELETE request removes the room from DataStore.rooms and returns 204 No Content.
If the same DELETE request is sent again, the room no longer exists. The server returns 404 Not Found. Even though the response code is different, the state of the system does not change because the room is already deleted.
According to RFC 7231, idempotency is about the effect on server state, not the response code.
If the room contains sensors, the DELETE request returns 409 Conflict and the room remains unchanged. Repeating the request will still return 409 Conflict, and the state of the room does not change.
Therefore, the operation is idempotent because repeated requests do not change the system state after the first attempt.


# Part 3: Sensor Operations & Linking

## 3.1 Sensor Resource & Integrity
Consequences of sending a non JSON payload to a @Consumes(MediaType.APPLICATION_JSON) endpoint
If a client sends a request with a Content-Type that is not application/json, the request will be rejected automatically by JAX-RS before reaching the createSensor method.
The server returns HTTP 415 Unsupported Media Type.
This happens because Jersey uses MessageBodyReader providers to convert incoming request bodies into Java objects. When a request arrives, Jersey looks for a reader that supports the request's media type and the expected Java object (Sensor).
The JacksonFeature registered in ApplicationConfig provides a reader for JSON only. If the request is not JSON, Jersey cannot deserialize it and immediately returns a 415 response.
This behaviour is useful because it enforces a strict contract between the client and the server. Clients must send JSON when interacting with the API.
The @Consumes(MediaType.APPLICATION_JSON) annotation clearly specifies what format the API accepts, while @Produces(MediaType.APPLICATION_JSON) ensures that responses are always returned in JSON format. This makes the API predictable and easier for clients to use.
________________________________________

## 3.2 Filtered Retrieval & Search
Query parameter versus path segment for filtering
This API uses a query parameter (@QueryParam("type")) to filter sensors by type, for example:
GET /sensors?type=CO2
Another possible design would be:
/sensors/type/CO2
However, query parameters are more appropriate for filtering.
Query parameters clearly indicate that the request is filtering the same resource collection, while path segments suggest that the request is accessing a different sub-resource.
Query parameters also support multiple filters easily, such as:
/sensors?type=CO2&status=ACTIVE
Using path segments for multiple filters would lead to complicated URLs and rigid endpoint definitions.
Query parameters are also naturally optional. If no filter is provided, the API simply returns all sensors.
For these reasons, REST best practices recommend using query parameters for filtering and path segments for identifying resources.
This makes the API more flexible, scalable, and easier to extend in the future.
________________________________________

# Part 4: Deep Nesting with Sub Resources

## 4.1 The Sub Resource Locator Pattern
Architectural benefits and complexity management
The sub-resource locator pattern is implemented using a method that has the @Path annotation but does not specify an HTTP method like @GET or @POST.
Instead, it returns another resource class that handles the nested endpoint.
Example:
@Path("/{sensorId}/readings")
public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId)
This design provides several advantages.
First, it improves separation of concerns. SensorResource handles sensor operations such as creating or listing sensors, while SensorReadingResource manages reading history.
Second, it allows context encapsulation. The sub-resource receives the sensorId when it is created, so all its methods can directly use it without repeatedly extracting path parameters.
Third, it improves code readability. Developers can easily locate reading-related logic inside SensorReadingResource.java instead of searching through a large file.
Fourth, the code structure reflects the API structure. The relationship /sensors/{id}/readings is represented by SensorResource leading to SensorReadingResource.
Finally, it makes testing easier, since each resource class can be tested independently.
Without this pattern, all endpoints would be placed inside one large resource class, creating a monolithic “god class” that is difficult to maintain and scale.
________________________________________

## 4.2 Historical Data Management
Implementation and side effect consistency
The SensorReadingResource provides two endpoints:
•	GET / – returns all readings for the sensor. 
•	POST / – adds a new reading. 
Before adding a reading, the API checks that the sensor exists and is not in MAINTENANCE mode.
After a reading is successfully added, the system updates the parent Sensor object’s currentValue field:
sensor.setCurrentValue(reading.getValue());
This ensures that the sensor always reflects the most recent measurement.
The implementation also automatically generates a UUID for the reading if none is provided and assigns the current timestamp if it is missing.
This keeps the API data consistent and reduces the amount of work required from clients.
________________________________________

# Part 5: Advanced Error Handling, Exception Mapping & Logging

## 5.1 Dependency Validation (422 Unprocessable Entity)
Why HTTP 422 is more accurate than 404 for a missing reference inside a valid JSON payload
HTTP 404 Not Found means that the requested URL does not exist. However, when a client sends:
POST /api/v1/sensors
the endpoint itself exists.
If the JSON payload contains a roomId that does not exist, the request format is correct, but the server cannot process it because the referenced room is invalid.
HTTP 422 Unprocessable Entity is more appropriate in this situation. It means that the server understands the request and the JSON syntax is valid, but it cannot process the request due to semantic errors.
Using 422 clearly communicates to the client that the data inside the request is incorrect, not the endpoint.
This approach is commonly used in modern REST APIs such as those from Stripe and Twilio.
The LinkedResourceNotFoundExceptionMapper in this project returns 422 to indicate this type of validation failure.
________________________________________

## 5.2 The Global Safety Net (500)
Cybersecurity risks of exposing internal Java stack traces
Exposing raw stack traces in API responses can create serious security risks.
Stack traces can reveal details about the technology stack, such as Java versions, frameworks like Jersey, and internal packages. Attackers can use this information to find known vulnerabilities.
They may also expose internal file paths and source code locations, which can help attackers understand the structure of the system.
Stack traces can also reveal business logic details, such as how validation works or how resources are processed. This information can be used to design targeted attacks.
In some cases, stack traces may even expose sensitive data.
To prevent these risks, the GlobalExceptionMapper catches all unexpected errors and returns a simple response:
{
 "error": "Internal Server Error",
 "message": "An unexpected error occurred. Please contact support."
}
The full stack trace is logged only on the server using java.util.logging.Logger.
This approach follows OWASP security recommendations, which suggest showing generic error messages to clients while logging detailed information internally.
________________________________________

## 5.3 API Request & Response Logging Filters
Advantages of JAX RS filters over manual logging in resource methods
Using ContainerRequestFilter and ContainerResponseFilter for logging provides several advantages compared to adding logging code inside each resource method.
First, it improves separation of concerns. Logging is an infrastructure task, not business logic. Filters keep resource classes clean and focused on their main responsibilities.
Second, it follows the DRY principle. Instead of writing logging code in every endpoint, filters centralize the logic in one place.
Third, filters ensure consistent logging. Every request and response is automatically logged, reducing the risk of missing logs in some endpoints.
Fourth, filters have access to complete request and response information, including headers, URI, and status codes.
Fifth, filters allow performance monitoring. They can capture request start and end times to measure API latency.
Finally, filters support an interceptor-style design, where multiple filters can be chained and applied conditionally.
In this project, the LoggingFilter records the HTTP method, request URI, and response status code for every API call, ensuring consistent and centralized logging without adding code to individual resource classes.
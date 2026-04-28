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
# Leader Lock Demo

Runnable Spring Boot 4 application demonstrating the `leader-lock-library` module.

## Run

Start Redis with port `6379` published to the host:

```powershell
docker run --rm --name leader-lock-redis -p 6379:6379 redis:7.4-alpine
```

Start the application:

```powershell
.\gradlew.bat :leader-lock-demo:bootRun
```

Run multiple instances with different ports and IDs:

```powershell
$env:INSTANCE_ID = 'demo-one'
$env:SERVER_PORT = '8080'
.\gradlew.bat :leader-lock-demo:bootRun
```

The demo module includes Windows scripts for this workflow:

```bat
leader-lock-demo\scripts\start-instances.bat 3 8080
leader-lock-demo\scripts\check-instances.bat 3 8080
```

## Endpoints

- `/swagger-ui.html` - Swagger UI
- `/v3/api-docs` - OpenAPI document
- `/api/leader` - current role
- `/api/health` - leadership and backend status
- `/api/health/leadership` - leadership status
- `/api/health/backend` - lock backend status
- `/actuator/health/readiness` - leader-ready health

The demo registers a `LeaderElectionListener`. Callback activity is logged as `[CALLBACK][LEADER]` or `[CALLBACK][STANDBY]`, and `/api/leader` returns the most recent callback transition in `lastTransition`.

## Integration tests

The application tests load the complete Spring context and use a test-only fake lock provider. They do not start Redis and do not use Testcontainers:

```powershell
.\gradlew.bat :leader-lock-demo:test
```

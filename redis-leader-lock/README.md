# Redis Leader Lock

Reusable leader election for Spring Boot 4 applications, backed by a pluggable lock-provider abstraction. The repository includes a Redisson/Redis provider and a runnable demonstration application.

## Modules

| Module                | Purpose                                                                                                                   |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `leader-lock-library` | Reusable Java 21+ Spring Boot auto-configuration, election lifecycle, health indicators, metrics, and lock-provider APIs. |
| `leader-lock-demo`    | Spring Boot 4 demonstration app with REST status endpoints, Swagger UI, and a guarded scheduled worker.                   |

The library is compiled with Java release 21 and can be used on Java 21 or newer. The example runs on the same Java 21-compatible baseline.

## Architecture

The election service depends on two backend-neutral interfaces:

```java
public interface LeaderLockProvider {
    LeaderLock getLock(String key);
}

public interface LeaderLock {
    boolean tryAcquire();
    boolean isOwnedByCurrentThread();
    void release();
}
```

The default implementation is `RedissonLeaderLockProvider`, which uses a shared Redis lock and Redisson watchdog renewal. A future database or local-file implementation can provide `LeaderLockProvider` without changing the election service.

Redis-compatible cluster or cloud deployments are supported with `deployment-mode: cluster`, `node-addresses`, `rediss://`, and optional JKS or PKCS12 truststore/keystore settings. Redisson uses its `CredentialsResolver` API for credentials. CockroachDB is not a Redis-compatible endpoint; connecting to CockroachDB requires a separate SQL-backed provider implementation.

## Use the library

Add the library artifact to a Spring Boot 4 application:

```groovy
dependencies {
    implementation 'com.nsauto:leader-lock-library:0.1.0'
}
```

The library auto-configures Redis leadership when `leader-lock-library` is on the classpath. Configure the lock with external properties:

```yaml
app:
  leader-lock:
    redis-url: ${REDIS_URL:redis://localhost:6379}
    deployment-mode: ${REDIS_DEPLOYMENT_MODE:single}
    node-addresses: ${REDIS_NODE_ADDRESSES:}
    tls-enabled: ${REDIS_TLS_ENABLED:false}
    username: ${REDIS_USERNAME:}
    password: ${REDIS_PASSWORD:}
    truststore-path: ${REDIS_TRUSTSTORE_PATH:}
    truststore-password: ${REDIS_TRUSTSTORE_PASSWORD:}
    keystore-path: ${REDIS_KEYSTORE_PATH:}
    keystore-password: ${REDIS_KEYSTORE_PASSWORD:}
    keystore-type: ${REDIS_KEYSTORE_TYPE:JKS}
    ssl-endpoint-identification: ${REDIS_SSL_ENDPOINT_IDENTIFICATION:true}
    key: ${LEADER_LOCK_KEY:app:leader-lock}
    retry-delay: ${LEADER_LOCK_RETRY_DELAY:5s}
    confirmation-interval: ${LEADER_LOCK_CONFIRMATION_INTERVAL:3s}
    shutdown-timeout: ${LEADER_LOCK_SHUTDOWN_TIMEOUT:5s}
    instance-id: ${INSTANCE_ID:}
```

Inject `LeaderElectionService` into leader-only work and confirm ownership immediately before a critical operation:

```java
if (leaderElectionService.confirmLeadership()) {
    processLeaderOnlyWork();
}
```

To provide another backend, register a `LeaderLockProvider` bean. It replaces the default Redis provider:

```java
@Bean
LeaderLockProvider fileLockProvider() {
    return new FileLeaderLockProvider(Path.of("/var/run/my-app.lock"));
}
```

Custom providers should preserve ownership safety and should make `isAvailable()` return `false` when the backend cannot confirm coordination state.

## Run the demo

Requirements: Java 21+ and Gradle, or Docker Desktop.

Start Redis with a host port mapping:

```powershell
docker run --rm --name leader-lock-redis -p 6379:6379 redis:7.4-alpine
```

Run the demo:

```powershell
.\gradlew.bat :leader-lock-demo:bootRun
```

The default Redis URL is `redis://localhost:6379`. Override the instance identity and HTTP port when running multiple local processes:

```powershell
$env:INSTANCE_ID = 'local-one'
$env:SERVER_PORT = '8080'
.\gradlew.bat :leader-lock-demo:bootRun
```

## Multi-instance testing on Windows

The included scripts launch multiple demo processes that share one lock key:

```bat
leader-lock-demo\scripts\start-instances.bat 3 8080
leader-lock-demo\scripts\check-instances.bat 3 8080
leader-lock-demo\scripts\stop-instances.bat
```

This starts instances on ports `8080`, `8081`, and `8082`; exactly one should report `LEADER`. To simulate Redis loss, use the running container name:

```bat
leader-lock-demo\scripts\simulate-redis-outage.bat leader-lock-redis 15
```

During the outage, the current leader must stop active work and return to standby. After Redis recovers, one instance can acquire leadership again.

## Container execution

The Compose file starts Redis and two demo instances:

```powershell
docker compose -f leader-lock-demo/docker-compose.yml up --build
```

The instances are available on ports `8080` and `8081`. Inside Compose, the application uses `redis://redis:6379`; host applications should use `redis://localhost:6379`.

## API and observability

Demo endpoints:

- `/swagger-ui.html` - interactive Swagger UI
- `/v3/api-docs` - OpenAPI JSON
- `/api/leader` - current instance role
- `/api/health` - leadership and lock-backend health
- `/api/health/leadership` - leadership health
- `/api/health/backend` - backend availability
- `/actuator/health` - aggregate health
- `/actuator/health/readiness` - active-ready only when Redis and leadership are confirmed
- `/actuator/metrics/leader.active` - current leadership gauge

Applications can react to transitions by registering a `LeaderElectionListener` bean. The library invokes `onElected(LeadershipEvent)` after confirmed acquisition and `onRemoved(LeadershipEvent)` after confirmed leadership loss or graceful shutdown. Listener failures are isolated from the election loop.

Leadership transitions are logged as colored `[LEADER]` and `[STANDBY]` messages when the terminal supports ANSI colors.

## Testing

No Testcontainers dependency is used. The library has unit tests using fake providers, and the demo has Spring application-context integration tests using a test-only provider:

```powershell
.\gradlew.bat test
```

The tests do not require Docker or a running Redis instance.

## GitHub Actions

The repository includes workflows under `.github/workflows/`:

- `ci.yml` runs the Java 21 Gradle test suite and builds the demo jar.
- `docker.yml` builds the demo container image using the multi-module Docker context.

Both workflows run safely for a private GitHub repository without publishing artifacts or requiring Redis credentials.

## Operational limitations

Leader election is coordination, not exactly-once processing. Make business operations idempotent and use unique operation identifiers. For systems that cannot tolerate stale leaders, consider fencing tokens and transactional business state. Redis Sentinel or Cluster deployments require topology-specific validation.

## License

Add the project license before publishing the repository publicly.

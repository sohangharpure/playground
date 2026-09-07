# Leader Lock Library

Reusable Spring Boot 4 auto-configuration for leader election. The library is compiled for Java 21 and is compatible with Java 21 and newer runtimes.

## Add the dependency

```groovy
dependencies {
    implementation 'com.nsauto:leader-lock-library:0.1.0'
}
```

The module can also be installed locally while developing the repository:

```powershell
.\gradlew.bat :leader-lock-library:publishToMavenLocal
```

## Backend abstraction

The election algorithm depends on `LeaderLockProvider`, not on Redis:

```java
public interface LeaderLockProvider {
    LeaderLock getLock(String key);
    default boolean isAvailable() { return true; }
}
```

`LeaderLock` defines the thread-owned operations needed by the election lifecycle:

```java
public interface LeaderLock {
    boolean tryAcquire();
    boolean isOwnedByCurrentThread();
    void release();
}
```

The default `RedissonLeaderLockProvider` uses Redis. An application can provide its own Spring bean to replace it with a database, file, or another distributed coordination backend:

```java
@Bean
LeaderLockProvider customProvider() {
    return new FileLeaderLockProvider(Path.of("/var/run/app.lock"));
}
```

Custom providers must make ownership checks authoritative, release only locks owned by the current execution context, and report backend failures through `isAvailable()`.

For a Redis-compatible cluster or cloud service, use `deployment-mode: cluster` and provide `node-addresses`. TLS can be enabled with a `rediss://` URL or `tls-enabled: true`. JKS and PKCS12 truststores and keystores are supported. Credentials use Redisson's `CredentialsResolver` API rather than deprecated direct username/password setters.

Redisson communicates using the Redis protocol. It cannot connect directly to CockroachDB (often abbreviated CRDB), because CockroachDB is a SQL database. A CockroachDB implementation would be a separate `LeaderLockProvider` backed by a transactional SQL lock or lease.

## Configuration

```yaml
app:
  leader-lock:
    redis-url: redis://localhost:6379
    deployment-mode: single
    node-addresses: []
    tls-enabled: false
    username: ${REDIS_USERNAME:}
    password: ${REDIS_PASSWORD:}
    truststore-path: ${REDIS_TRUSTSTORE_PATH:}
    truststore-password: ${REDIS_TRUSTSTORE_PASSWORD:}
    keystore-path: ${REDIS_KEYSTORE_PATH:}
    keystore-password: ${REDIS_KEYSTORE_PASSWORD:}
    keystore-type: JKS
    ssl-endpoint-identification: true
    key: app:leader-lock
    retry-delay: 5s
    confirmation-interval: 3s
    shutdown-timeout: 5s
    instance-id: node-a
```

The library starts passively, retries acquisition, confirms ownership before active work, and releases the lock safely during graceful shutdown. `LeaderElectionService#confirmLeadership()` should be called immediately before important leader-only operations.

## Provided beans

- `LeaderElectionService`
- `LeadershipHealthIndicator`, exposed as Actuator contributor `leadership`
- `RedisConnectionHealthIndicator`, exposed as Actuator contributor `lockBackend`
- `LeaderLockProvider`, defaulting to Redisson/Redis

## Leadership callbacks

Register a `LeaderElectionListener` bean to react to transitions:

```java
@Component
class WorkLifecycleListener implements LeaderElectionListener {
    @Override
    public void onElected(LeadershipEvent event) {
        startLeaderWork(event.instanceId());
    }

    @Override
    public void onRemoved(LeadershipEvent event) {
        stopLeaderWork(event.reason());
    }
}
```

Each `LeadershipEvent` contains the instance ID, lock key, reason, and UTC timestamp. Listener exceptions are caught and logged by the library so they cannot interrupt lock renewal or election. Callbacks run on the election control thread, so long-running work should be handed off to an application executor.

Metrics include acquisition attempts, successes, failures, acquisition latency, leadership losses, and the current active gauge.

## Tests

The library tests use fake providers and do not require Docker, Redis, or Testcontainers:

```powershell
.\gradlew.bat :leader-lock-library:test
```

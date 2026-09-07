package com.nsauto.leaderlock.redisson;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.Credentials;
import org.redisson.config.EqualJitterDelay;
import org.redisson.config.SslProvider;
import org.redisson.config.SslVerificationMode;

import com.nsauto.leaderlock.LeaderLock;
import com.nsauto.leaderlock.LeaderLockProperties;
import com.nsauto.leaderlock.LeaderLockProvider;

public final class RedissonLeaderLockProvider implements LeaderLockProvider, AutoCloseable {

    private final RedissonClient redissonClient;

    public RedissonLeaderLockProvider(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public static RedissonLeaderLockProvider create(LeaderLockProperties properties) {
        return new RedissonLeaderLockProvider(createClient(properties));
    }

    @Override
    public LeaderLock getLock(String key) {
        return new RedissonLeaderLock(redissonClient.getLock(key));
    }

    @Override
    public boolean isAvailable() {
        try {
            redissonClient.getBucket("app:leader-lock:health-check").isExists();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public String backendName() {
        return "redisson";
    }

    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    @Override
    public void close() {
        redissonClient.shutdown();
    }

    private static RedissonClient createClient(LeaderLockProperties properties) {
        URI uri = URI.create(properties.getRedisUrl());
        Config config = new Config();
        config.setLazyInitialization(true);
        if ("cluster".equalsIgnoreCase(properties.getDeploymentMode())) {
            List<String> nodes = properties.getNodeAddresses().isEmpty()
                    ? List.of(toRedissonAddress(uri))
                    : properties.getNodeAddresses().stream().map(RedissonLeaderLockProvider::normalizeAddress).toList();
                config.useClusterServers()
                    .addNodeAddress(nodes.toArray(String[]::new))
                    .setScanInterval(properties.getClusterScanInterval())
                    .setConnectTimeout(3_000)
                    .setTimeout(3_000)
                    .setRetryAttempts(3);
        } else {
            config.useSingleServer()
                    .setAddress(toRedissonAddress(uri))
                    .setConnectTimeout(3_000)
                    .setTimeout(3_000)
                    .setRetryAttempts(3)
                        .setRetryDelay(new EqualJitterDelay(java.time.Duration.ofMillis(500),
                            java.time.Duration.ofSeconds(2)))
                        .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(8);
        }

        configureCredentials(config, properties, uri);
        configureTls(config, properties, uri);
        return Redisson.create(config);
    }

    private static void configureCredentials(Config config, LeaderLockProperties properties, URI uri) {
        String username = properties.getUsername();
        String password = properties.getPassword();
        if ((username == null || username.isBlank()) && uri.getUserInfo() != null) {
            String[] credentials = uri.getUserInfo().split(":", 2);
            username = credentials.length == 2 ? credentials[0] : "";
            password = credentials.length == 2 ? credentials[1] : credentials[0];
        }
        if (password != null && !password.isBlank()) {
            String resolvedUsername = username == null ? "" : username;
            String resolvedPassword = password;
            config.setCredentialsResolver(address -> CompletableFuture.completedFuture(
                    new Credentials(resolvedUsername, resolvedPassword)));
        }
    }

    private static void configureTls(Config config, LeaderLockProperties properties, URI uri) {
        boolean tlsEnabled = properties.isTlsEnabled() || "rediss".equalsIgnoreCase(uri.getScheme());
        if (!tlsEnabled) {
            return;
        }
        config.setSslProvider(SslProvider.JDK);
        config.setSslVerificationMode(properties.isSslEndpointIdentification()
            ? SslVerificationMode.STRICT
            : SslVerificationMode.NONE);
        if (properties.getTruststorePath() != null && !properties.getTruststorePath().isBlank()) {
            config.setSslTruststore(toUrl(properties.getTruststorePath()));
            config.setSslTruststorePassword(properties.getTruststorePassword());
        }
        if (properties.getKeystorePath() != null && !properties.getKeystorePath().isBlank()) {
            config.setSslKeystore(toUrl(properties.getKeystorePath()));
            config.setSslKeystorePassword(properties.getKeystorePassword());
            config.setSslKeystoreType(properties.getKeystoreType());
        }
    }

    private static URL toUrl(String location) {
        try {
            if (location.contains("://")) {
                return URI.create(location).toURL();
            }
            return Path.of(location).toUri().toURL();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid TLS store location: " + location, exception);
        }
    }

    private static String normalizeAddress(String address) {
        return address.contains("://") ? address : "redis://" + address;
    }

    private static String toRedissonAddress(URI uri) {
        String scheme = "rediss".equalsIgnoreCase(uri.getScheme()) ? "rediss://" : "redis://";
        String host = uri.getHost() == null ? "localhost" : uri.getHost();
        int port = uri.getPort() < 0 ? 6379 : uri.getPort();
        return scheme + host + ":" + port;
    }

    private record RedissonLeaderLock(RLock delegate) implements LeaderLock {
        @Override
        public boolean tryAcquire() {
            return delegate.tryLock();
        }

        @Override
        public boolean isOwnedByCurrentThread() {
            return delegate.isHeldByCurrentThread();
        }

        @Override
        public void release() {
            delegate.unlock();
        }
    }
}
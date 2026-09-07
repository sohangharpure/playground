package com.nsauto.leaderlock;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

public class RedisConnectionHealthIndicator implements HealthIndicator {

    private final LeaderLockProvider lockProvider;

    public RedisConnectionHealthIndicator(LeaderLockProvider lockProvider) {
        this.lockProvider = lockProvider;
    }

    @Override
    public Health health() {
        try {
            if (lockProvider.isAvailable()) {
                return Health.up().withDetail("backend", lockProvider.backendName()).build();
            }
            return Health.down().withDetail("backend", lockProvider.backendName()).build();
        } catch (RuntimeException exception) {
            return Health.down(exception).withDetail("redis", "unreachable").build();
        }
    }
}
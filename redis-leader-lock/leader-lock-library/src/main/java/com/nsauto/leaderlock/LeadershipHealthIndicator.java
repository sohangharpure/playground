package com.nsauto.leaderlock;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

public class LeadershipHealthIndicator implements HealthIndicator {

    private final LeaderElectionService leaderElectionService;

    public LeadershipHealthIndicator(LeaderElectionService leaderElectionService) {
        this.leaderElectionService = leaderElectionService;
    }

    @Override
    public Health health() {
        if (leaderElectionService.isActive()) {
            return Health.up().withDetail("instanceId", leaderElectionService.getInstanceId())
                    .withDetail("role", "LEADER").build();
        }
        return Health.down().withDetail("instanceId", leaderElectionService.getInstanceId())
                .withDetail("role", "STANDBY")
                .withDetail("reason", "Leadership is not confirmed").build();
    }
}
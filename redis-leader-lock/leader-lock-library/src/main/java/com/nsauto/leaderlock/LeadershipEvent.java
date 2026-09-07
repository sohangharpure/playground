package com.nsauto.leaderlock;

import java.time.Instant;

public record LeadershipEvent(
        String instanceId,
        String lockKey,
        String reason,
        Instant occurredAt) {
}
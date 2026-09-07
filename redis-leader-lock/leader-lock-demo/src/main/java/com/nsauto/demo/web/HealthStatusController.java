package com.nsauto.demo.web;

import com.nsauto.leaderlock.LeadershipHealthIndicator;
import com.nsauto.leaderlock.RedisConnectionHealthIndicator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.health.contributor.Health;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Application health and backend connectivity")
public class HealthStatusController {

    private final LeadershipHealthIndicator leadership;
    private final RedisConnectionHealthIndicator backend;

    public HealthStatusController(LeadershipHealthIndicator leadership, RedisConnectionHealthIndicator backend) {
        this.leadership = leadership;
        this.backend = backend;
    }

    @GetMapping
    @Operation(summary = "Get application health", description = "Returns leadership and lock-backend health checks together.")
    public HealthSnapshot health() {
        return new HealthSnapshot(leadership.health(), backend.health());
    }

    @GetMapping("/leadership")
    @Operation(summary = "Get leadership health")
    public Health leadership() {
        return leadership.health();
    }

    @GetMapping("/backend")
    @Operation(summary = "Get lock backend health")
    public Health backend() {
        return backend.health();
    }

    public record HealthSnapshot(Health leadership, Health backend) {
    }
}
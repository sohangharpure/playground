package com.nsauto.demo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nsauto.leaderlock.LeaderElectionService;
import com.nsauto.demo.leadership.DemoLeadershipListener;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/leader")
@Tag(name = "Leader", description = "Leader-election runtime status")
public class LeaderStatusController {

    private final LeaderElectionService leaderElectionService;
    private final DemoLeadershipListener leadershipListener;

    public LeaderStatusController(LeaderElectionService leaderElectionService,
            DemoLeadershipListener leadershipListener) {
        this.leaderElectionService = leaderElectionService;
        this.leadershipListener = leadershipListener;
    }

    @GetMapping
    @Operation(summary = "Get leader status", description = "Returns whether this instance currently owns the Redis leader lock.")
    public LeaderStatus status() {
        boolean active = leaderElectionService.isActive();
        return new LeaderStatus(leaderElectionService.getInstanceId(), active,
                active ? "LEADER" : "STANDBY", leadershipListener.getLastTransition());
    }

    public record LeaderStatus(String instanceId, boolean active, String role,
            DemoLeadershipListener.Transition lastTransition) {
    }
}
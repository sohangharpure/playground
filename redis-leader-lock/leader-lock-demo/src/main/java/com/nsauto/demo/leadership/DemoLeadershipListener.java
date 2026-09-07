package com.nsauto.demo.leadership;

import java.util.concurrent.atomic.AtomicReference;

import com.nsauto.leaderlock.LeaderElectionListener;
import com.nsauto.leaderlock.LeadershipEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemoLeadershipListener implements LeaderElectionListener {

    private static final Logger log = LoggerFactory.getLogger(DemoLeadershipListener.class);

    private final AtomicReference<Transition> lastTransition = new AtomicReference<>();

    @Override
    public void onElected(LeadershipEvent event) {
        lastTransition.set(new Transition("LEADER", event));
        log.info("[CALLBACK][LEADER] Leadership callback received: instanceId={}, reason={}, occurredAt={}",
                event.instanceId(), event.reason(), event.occurredAt());
    }

    @Override
    public void onRemoved(LeadershipEvent event) {
        lastTransition.set(new Transition("STANDBY", event));
        log.info("[CALLBACK][STANDBY] Leadership removal callback received: instanceId={}, reason={}, occurredAt={}",
                event.instanceId(), event.reason(), event.occurredAt());
    }

    public Transition getLastTransition() {
        return lastTransition.get();
    }

    public record Transition(String role, LeadershipEvent event) {
    }
}

package com.nsauto.demo.work;

import java.util.concurrent.atomic.AtomicLong;

import com.nsauto.leaderlock.LeaderElectionService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeaderOnlyWorker {

    private static final Logger log = LoggerFactory.getLogger(LeaderOnlyWorker.class);

    private final LeaderElectionService leaderElectionService;
    private final AtomicLong workRuns = new AtomicLong();

    public LeaderOnlyWorker(LeaderElectionService leaderElectionService, MeterRegistry meterRegistry) {
        this.leaderElectionService = leaderElectionService;
        meterRegistry.gauge("leader.work.runs", workRuns);
    }

    @Scheduled(fixedDelayString = "${app.worker.interval:2s}")
    public void run() {
        if (!leaderElectionService.confirmLeadership()) {
            return;
        }
        long runNumber = workRuns.incrementAndGet();
        log.info("Leader-only work executed, instanceId={}, run={}",
                leaderElectionService.getInstanceId(), runNumber);
    }
}
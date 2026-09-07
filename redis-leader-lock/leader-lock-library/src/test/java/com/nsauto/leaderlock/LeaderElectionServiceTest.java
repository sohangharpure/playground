package com.nsauto.leaderlock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class LeaderElectionServiceTest {

    @Test
    void serviceAcceptsANonRedisLockProvider() {
        FakeLock lock = new FakeLock();
        LeaderLockProvider provider = new FakeLockProvider(lock);
        LeaderLockProperties properties = new LeaderLockProperties();
        properties.setKey("unit-test-lock");

        LeaderElectionService service = new LeaderElectionService(provider, properties, new SimpleMeterRegistry());

        assertThat(service.isActive()).isFalse();
        assertThat(service.confirmLeadership()).isFalse();
        assertThat(lock.acquireCalls).isZero();
    }

    @Test
    void notifiesListenerWhenLeadershipIsAcquiredAndRemoved() throws Exception {
        FakeLock lock = new FakeLock();
        lock.acquires = true;
        List<String> transitions = new ArrayList<>();
        LeaderElectionListener listener = new LeaderElectionListener() {
            @Override
            public void onElected(LeadershipEvent event) {
                transitions.add("elected:" + event.reason());
            }

            @Override
            public void onRemoved(LeadershipEvent event) {
                transitions.add("removed:" + event.reason());
            }
        };
        LeaderLockProperties properties = new LeaderLockProperties();
        properties.setConfirmationInterval(Duration.ofMillis(100));

        LeaderElectionService service = new LeaderElectionService(
                new FakeLockProvider(lock), properties, new SimpleMeterRegistry(), List.of(listener));
        service.start();
        try {
            await().until(() -> service.isActive());
            assertThat(transitions).hasSize(1);
            assertThat(transitions.get(0)).startsWith("elected:");

            lock.owned = false;
            lock.acquires = false;
            await().until(() -> !service.isActive());
            assertThat(transitions).hasSize(2);
            assertThat(transitions.get(1)).startsWith("removed:");
        } finally {
            service.stop();
        }
    }

    private static Await await() {
        return new Await();
    }

    private static final class Await {
        void until(java.util.function.BooleanSupplier condition) throws InterruptedException {
            long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
            while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertThat(condition.getAsBoolean()).isTrue();
        }
    }

    private record FakeLockProvider(FakeLock lock) implements LeaderLockProvider {
        @Override
        public LeaderLock getLock(String key) {
            return lock;
        }

        @Override
        public String backendName() {
            return "fake";
        }
    }

    private static final class FakeLock implements LeaderLock {
        private int acquireCalls;
        private boolean acquires;
        private boolean owned;

        @Override
        public boolean tryAcquire() {
            acquireCalls++;
            owned = acquires;
            return acquires;
        }

        @Override
        public boolean isOwnedByCurrentThread() {
            return owned;
        }

        @Override
        public void release() {
        }
    }
}
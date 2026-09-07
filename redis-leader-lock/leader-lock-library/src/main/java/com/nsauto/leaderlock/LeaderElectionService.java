package com.nsauto.leaderlock;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class LeaderElectionService {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionService.class);
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private final LeaderLock lock;
    private final LeaderLockProperties properties;
    private final ScheduledExecutorService controlExecutor;
    private final AtomicBoolean active = new AtomicBoolean();
    private final AtomicInteger activeGauge = new AtomicInteger();
    private final Counter acquisitionAttempts;
    private final Counter acquisitionSuccesses;
    private final Counter acquisitionFailures;
    private final Counter leadershipLosses;
    private final Timer acquisitionLatency;
    private final List<LeaderElectionListener> listeners;
    private final String instanceId;
    private volatile boolean stopping;
    private volatile ScheduledFuture<?> nextCycle;
    private volatile Thread controlThread;

    public LeaderElectionService(LeaderLockProvider lockProvider, LeaderLockProperties properties,
            MeterRegistry meterRegistry) {
        this(lockProvider, properties, meterRegistry, List.of());
    }

    public LeaderElectionService(LeaderLockProvider lockProvider, LeaderLockProperties properties,
            MeterRegistry meterRegistry, List<LeaderElectionListener> listeners) {
        this.lock = lockProvider.getLock(properties.getKey());
        this.properties = properties;
        this.listeners = List.copyOf(listeners);
        this.controlExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "leader-lock-control");
            thread.setDaemon(true);
            return thread;
        });
        this.instanceId = properties.getInstanceId().isBlank()
                ? UUID.randomUUID().toString()
                : properties.getInstanceId();
        this.acquisitionAttempts = Counter.builder("leader.lock.acquisition.attempts")
                .tag("instance", instanceId).register(meterRegistry);
        this.acquisitionSuccesses = Counter.builder("leader.lock.acquisition.successes")
                .tag("instance", instanceId).register(meterRegistry);
        this.acquisitionFailures = Counter.builder("leader.lock.acquisition.failures")
                .tag("instance", instanceId).register(meterRegistry);
        this.leadershipLosses = Counter.builder("leader.lock.losses")
                .tag("instance", instanceId).register(meterRegistry);
        this.acquisitionLatency = Timer.builder("leader.lock.acquisition.latency")
                .tag("instance", instanceId).register(meterRegistry);
        meterRegistry.gauge("leader.active", activeGauge);
    }

    @PostConstruct
    void start() {
        controlExecutor.execute(() -> {
            controlThread = Thread.currentThread();
            scheduleNext(Duration.ZERO);
        });
        log.info("{}[STANDBY]{} Leader election started in passive mode, instanceId={}, lockKey={}",
                RED, RESET, instanceId, properties.getKey());
    }

    public boolean isActive() {
        return active.get();
    }

    public String getInstanceId() {
        return instanceId;
    }

    public boolean confirmLeadership() {
        if (!active.get() || stopping) {
            return false;
        }
        if (Thread.currentThread() == controlThread) {
            return confirmOnControlThread();
        }
        Future<Boolean> confirmation = controlExecutor.submit(this::confirmOnControlThread);
        try {
            return confirmation.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception exception) {
            demote("leadership confirmation timed out or failed: " + exception.getClass().getSimpleName());
            return false;
        }
    }

    private boolean confirmOnControlThread() {
        try {
            boolean owned = lock.isOwnedByCurrentThread();
            if (!owned) {
                demote("ownership confirmation returned false");
            }
            return owned;
        } catch (RuntimeException exception) {
            demote("ownership confirmation failed: " + exception.getClass().getSimpleName());
            return false;
        }
    }

    private void electionCycle() {
        if (stopping) {
            return;
        }
        controlThread = Thread.currentThread();
        if (active.get()) {
            confirmOnControlThread();
            scheduleNext(properties.getConfirmationInterval());
            return;
        }

        acquisitionAttempts.increment();
        Timer.Sample sample = Timer.start();
        try {
            boolean acquired = lock.tryAcquire();
            sample.stop(acquisitionLatency);
            if (acquired) {
                acquisitionSuccesses.increment();
                active.set(true);
                activeGauge.set(1);
                log.info("{}[LEADER]{} Leadership acquired, instanceId={}, lockKey={}",
                        GREEN, RESET, instanceId, properties.getKey());
                notifyElected();
                scheduleNext(properties.getConfirmationInterval());
            } else {
                scheduleNext(properties.getRetryDelay());
            }
        } catch (RuntimeException exception) {
            sample.stop(acquisitionLatency);
            acquisitionFailures.increment();
            demote("leader acquisition failed: " + exception.getClass().getSimpleName());
            scheduleNext(properties.getRetryDelay());
        }
    }

    private void scheduleNext(Duration delay) {
        if (!stopping) {
            nextCycle = controlExecutor.schedule(this::electionCycle,
                    Math.max(0, delay.toMillis()), TimeUnit.MILLISECONDS);
        }
    }

    private void demote(String reason) {
        if (active.compareAndSet(true, false)) {
            activeGauge.set(0);
            leadershipLosses.increment();
            log.warn("{}[STANDBY]{} Leadership lost, instanceId={}, reason={}", RED, RESET, instanceId, reason);
            notifyRemoved(reason);
        }
    }

    private void notifyElected() {
        notifyListeners(listener -> listener.onElected(newEvent("leadership acquired")));
    }

    private void notifyRemoved(String reason) {
        notifyListeners(listener -> listener.onRemoved(newEvent(reason)));
    }

    private LeadershipEvent newEvent(String reason) {
        return new LeadershipEvent(instanceId, properties.getKey(), reason, java.time.Instant.now());
    }

    private void notifyListeners(java.util.function.Consumer<LeaderElectionListener> callback) {
        for (LeaderElectionListener listener : listeners) {
            try {
                callback.accept(listener);
            } catch (RuntimeException exception) {
                log.warn("Leadership listener failed, instanceId={}, listener={}",
                        instanceId, listener.getClass().getName(), exception);
            }
        }
    }

    @PreDestroy
    void stop() {
        stopping = true;
        if (active.compareAndSet(true, false)) {
            activeGauge.set(0);
            log.info("{}[STANDBY]{} Leadership stopped during shutdown, instanceId={}", RED, RESET, instanceId);
            notifyRemoved("application shutdown");
        }
        if (nextCycle != null) {
            nextCycle.cancel(false);
        }
        Future<?> unlock = controlExecutor.submit(() -> {
            if (lock.isOwnedByCurrentThread()) {
                lock.release();
                log.info("Released leader lock during shutdown, instanceId={}", instanceId);
            }
        });
        try {
            unlock.get(properties.getShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while releasing leader lock, instanceId={}", instanceId);
        } catch (ExecutionException | java.util.concurrent.TimeoutException exception) {
            log.warn("Could not release leader lock during shutdown; TTL recovery remains available, instanceId={}",
                    instanceId);
        } finally {
            controlExecutor.shutdownNow();
        }
    }
}
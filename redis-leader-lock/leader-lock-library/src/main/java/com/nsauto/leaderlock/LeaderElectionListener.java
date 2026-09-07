package com.nsauto.leaderlock;

/**
 * Receives leadership transition callbacks from the election service.
 * Listener failures are isolated and never interrupt lock management.
 */
public interface LeaderElectionListener {

    default void onElected(LeadershipEvent event) {
    }

    default void onRemoved(LeadershipEvent event) {
    }
}
package com.nsauto.leaderlock;

/**
 * Factory and availability contract for a leader-lock backend.
 * Applications can replace the default Redis implementation by providing this
 * interface as a Spring bean.
 */
public interface LeaderLockProvider {

    LeaderLock getLock(String key);

    default boolean isAvailable() {
        return true;
    }

    default String backendName() {
        return getClass().getSimpleName();
    }
}
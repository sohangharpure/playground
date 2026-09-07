package com.nsauto.leaderlock;

/**
 * A thread-owned lock used by the leader-election algorithm.
 * Implementations must keep acquire, ownership checks, and release compatible
 * with the execution context in which they are called.
 */
public interface LeaderLock {

    boolean tryAcquire();

    boolean isOwnedByCurrentThread();

    void release();
}
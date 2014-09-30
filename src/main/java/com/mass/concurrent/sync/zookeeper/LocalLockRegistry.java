package com.mass.concurrent.sync.zookeeper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mass.concurrent.LockRegistry;
import com.mass.concurrent.sync.SynchronizerLockKey;
import com.mass.concurrent.sync.SynchronizerLockKeyFactory;
import com.mass.core.PositiveDuration;
import com.sun.istack.internal.Nullable;

/**
 * This is a fake interprocess lock registry that does all locking in memory. This will protect a single JVM, but it's
 * unsuitable for use in a cluster.
 * 
 * @author kmassaroni
 * @param <K>
 */
class LocalLockRegistry<K> implements LockRegistry<K> {
    private final Cache<SynchronizerLockKey, ReentrantLock> locks = CacheBuilder.newBuilder().softValues().build();
    private final LockFactory lockFactory = new LockFactory();
    private final SynchronizerLockKeyFactory<K> lockKeyFactory;
    private final PositiveDuration timeoutDuration;

    public LocalLockRegistry(final SynchronizerLockKeyFactory<K> lockKeyFactory,
            final @Nullable PositiveDuration timeoutDuration) {
        Preconditions.checkArgument(lockKeyFactory != null, "Undefined lock key factory.");
        this.lockKeyFactory = lockKeyFactory;
        this.timeoutDuration = timeoutDuration;
    }

    @Override
    public PositiveDuration getTimeoutDuration() {
        return timeoutDuration;
    }

    @Override
    public ReentrantLock getLock(final K key) {
        Preconditions.checkArgument(key != null, "Undefined key.");

        final SynchronizerLockKey lockKey = lockKeyFactory.toKey(key);
        Preconditions.checkArgument(lockKey != null, "Lock factory produced a null lock key.");

        try {
            return locks.get(lockKey, lockFactory);
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static class LockFactory implements Callable<ReentrantLock> {
        @Override
        public ReentrantLock call() throws Exception {
            return new ReentrantLock();
        }
    }
}

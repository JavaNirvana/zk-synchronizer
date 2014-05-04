package com.mass.concurrent.sync.zookeeper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mass.concurrent.sync.zookeeper.BestEffortInterProcessReentrantLock.InterProcessLockFailObserver;

/**
 * This is designed so that zookeeper is not a single point of failure. If there's no zookeeper client available, then
 * this produces regular java reentrant locks. If there's a connectivity problem, or some other zookeeper configuration
 * problem, then these locks will log exceptions, but they won't throw them. These locks guarantee the java concurrent
 * reentrant lock contract, and make a best-effort attempt to provide the curator interprocess mutex contract.
 * 
 * @author kmassaroni
 */
public class BestEffortInterProcessReentrantLockRegistry {
    private final Cache<InterProcessLockKey, ReentrantLock> locks = CacheBuilder.newBuilder().softValues().build();
    private final String rootZkPath;
    private final CuratorFramework zkClient;

    private final Log log = LogFactory.getLog(BestEffortInterProcessReentrantLockRegistry.class);
    private final InterProcessLockFailObserver observer = new InterProcessLockFailObserver() {
        @Override
        public void onInterProcessLockFail(final Throwable failure) {
            log.error("Inter process locking failed.", failure);
        }
    };

    public BestEffortInterProcessReentrantLockRegistry(final String rootZkPath, final CuratorFramework zkClient) {
        super();
        this.rootZkPath = toZkRootPath(rootZkPath);
        this.zkClient = zkClient;
    }

    private static String toZkRootPath(final String path) {
        com.mass.core.Preconditions.checkNotBlank(path, "Undefined dir path for zookeeper mutexes base dir.");

        if (path.endsWith("/")) {
            return path;
        }

        return path + '/';
    }

    public ReentrantLock getLock(final InterProcessLockKey key) {
        Preconditions.checkArgument(key != null, "Undefined key.");
        final LockFactory factory = new LockFactory(key);

        try {
            return locks.get(key, factory);
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private class LockFactory implements Callable<ReentrantLock> {
        private final InterProcessLockKey id;

        public LockFactory(final InterProcessLockKey id) {
            this.id = id;
        }

        @Override
        public ReentrantLock call() throws Exception {
            final String path = rootZkPath + id.getValue();

            if (zkClient == null) {
                return new ReentrantLock(true);
            }

            final InterProcessMutex mutex = new InterProcessMutex(zkClient, path);
            final BestEffortInterProcessReentrantLock lock = new BestEffortInterProcessReentrantLock(mutex, observer);
            return lock;
        }
    }

}
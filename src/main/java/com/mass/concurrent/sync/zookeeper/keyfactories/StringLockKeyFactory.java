package com.mass.concurrent.sync.zookeeper.keyfactories;

import com.mass.codec.Base64;
import com.mass.concurrent.sync.zookeeper.InterProcessLockKey;
import com.mass.concurrent.sync.zookeeper.InterProcessLockKeyFactory;
import com.mass.core.Preconditions;

/**
 * Use strings as keys to interprocess locks. Lock keys are Base64'd, because not all strings can be valid zookeeper
 * paths.
 * 
 * @author kmassaroni
 */
public class StringLockKeyFactory implements InterProcessLockKeyFactory<String> {
    @Override
    public InterProcessLockKey toKey(final String key) {
        Preconditions.checkNotEmpty(key, "Empty interprocess lock key.");
        final String zkSafe = Base64.encodeURLSafe(key);
        return new InterProcessLockKey(zkSafe);
    }
}
package org.terracotta.modules.ehcache.concurrency;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.Sync;

import org.terracotta.locking.ClusteredLock;
import org.terracotta.modules.ehcache.store.ClusteredStore.SyncLockState;
import org.terracotta.modules.ehcache.store.ClusteredStoreBackend;
import org.terracotta.modules.ehcache.store.ValueModeHandler;

/**
 * @author Alex Snaps
 */
public class TcCacheLockProvider implements CacheLockProvider {

  private final ClusteredStoreBackend      backend;
  private final ValueModeHandler           valueModeHandler;
  private final ThreadLocal<SyncLockState> synclockstate;

  public TcCacheLockProvider(ThreadLocal<SyncLockState> synclockstate, final ClusteredStoreBackend backend,
                             final ValueModeHandler valueModeHandler) {

    this.synclockstate = synclockstate;
    this.backend = backend;
    this.valueModeHandler = valueModeHandler;
  }

  public Sync getSyncForKey(final Object key) {
    return new TcSync(synclockstate, getFinegrainedLock(key));
  }

  private ClusteredLock getFinegrainedLock(final Object key) {
    Object portableKey;

    try {
      portableKey = valueModeHandler.createPortableKey(key);
    } catch (Exception e) {
      throw new CacheException(e);
    }

    return backend.createFinegrainedLock(portableKey);
  }
}

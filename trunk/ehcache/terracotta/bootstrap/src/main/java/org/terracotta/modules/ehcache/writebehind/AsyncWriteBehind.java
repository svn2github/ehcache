/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.OperationsFilter;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.terracotta.modules.ehcache.async.AsyncCoordinator;
import org.terracotta.modules.ehcache.async.scatterpolicies.ItemScatterPolicy;
import org.terracotta.modules.ehcache.writebehind.operations.DeleteAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.SingleAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.WriteAsyncOperation;

public class AsyncWriteBehind implements WriteBehind {
  private final AsyncCoordinator<SingleAsyncOperation> async;
  private final int                                    concurrency;

  /**
   * Instantiate a new instance of {@code AsyncWriteBehind} by providing the async coordinator instance that will be
   * used for the underlying behavior.
   * 
   * @param async the async coordinator instance that will be used by the write behind queue
   * @param cache the cache this write behind is bound to
   */
  public AsyncWriteBehind(AsyncCoordinator async, Ehcache cache) {
    this.async = async;
    this.concurrency = cache.getCacheConfiguration().getCacheWriterConfiguration().getWriteBehindConcurrency();
  }

  @Override
  public void start(CacheWriter writer) throws CacheException {
    async.start(new CacheWriterProcessor(writer), concurrency, new SingleAsyncOperationItemScatterPolicy());
  }

  // This method is to be called from within a clustered Lock as it does not take any clustered lock inside.
  @Override
  public void write(Element element) {
    async.add(new WriteAsyncOperation(element));
  }

  // This method is to be called from within a clustered Lock as it does not take any clustered lock inside.
  @Override
  public void delete(CacheEntry entry) {
    async.add(new DeleteAsyncOperation(entry.getKey(), entry.getElement()));
  }

  @Override
  public void setOperationsFilter(OperationsFilter filter) {
    OperationsFilterWrapper filterWrapper = new OperationsFilterWrapper(filter);
    async.setOperationsFilter(filterWrapper);
  }

  @Override
  public void stop() throws CacheException {
    async.stop();
  }

  @Override
  public long getQueueSize() {
    return async.getQueueSize();
  }

  private static class SingleAsyncOperationItemScatterPolicy implements ItemScatterPolicy<SingleAsyncOperation> {
    private SingleAsyncOperationItemScatterPolicy() {
      //
    }

    @Override
    public int selectBucket(final int count, final SingleAsyncOperation item) {
      Object key;
      try {
        key = item.getKey();
      } catch (Exception e) {
        throw new CacheException(e);
      }
      return Math.abs(key.hashCode() % count);
    }
  }

}

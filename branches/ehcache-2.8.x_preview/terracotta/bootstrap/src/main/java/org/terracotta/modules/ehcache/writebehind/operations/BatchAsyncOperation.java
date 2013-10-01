/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.operations;

import net.sf.ehcache.writer.CacheWriter;

/**
 * Interface to implement batch operations that are executed on a cache writer after being called by the async
 * coordinator
 * 
 * @author Geert Bevin
 * @version $Id: BatchAsyncOperation.java 5227 2012-02-02 02:35:49Z hhuynh $
 */
public interface BatchAsyncOperation {
  /**
   * Perform the batch operation for a particular batch writer
   * 
   * @param cacheWriter the cache writer this operation should be performed upon
   */
  void performBatchOperation(CacheWriter cacheWriter);

}

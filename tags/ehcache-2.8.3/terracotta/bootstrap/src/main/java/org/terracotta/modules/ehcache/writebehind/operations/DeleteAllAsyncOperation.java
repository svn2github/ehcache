/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.operations;


import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.writer.CacheWriter;

import java.util.Collection;

/**
 * Implements the delete all operation for write behind
 * 
 * @author Abhishek Maheshwari
 */
public class DeleteAllAsyncOperation implements BatchAsyncOperation {
  private final Collection<CacheEntry> entries;

  public DeleteAllAsyncOperation(Collection<CacheEntry> entries) {
    this.entries = entries;
  }

  public void performBatchOperation(CacheWriter cacheWriter) {
    cacheWriter.deleteAll(entries);
  }
}

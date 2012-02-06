/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.operations;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.writer.CacheWriter;

import java.util.List;

/**
 * Implements the delete all operation for write behind
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class DeleteAllAsyncOperation implements BatchAsyncOperation {
  private final List<CacheEntry> entries;

  public DeleteAllAsyncOperation(List<CacheEntry> entries) {
    this.entries = entries;
  }

  public void performBatchOperation(CacheWriter cacheWriter) {
    cacheWriter.deleteAll(entries);
  }
}

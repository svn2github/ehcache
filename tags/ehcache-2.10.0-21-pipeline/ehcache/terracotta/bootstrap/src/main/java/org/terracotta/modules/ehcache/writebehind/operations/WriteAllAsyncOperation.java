/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.operations;


import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;

import java.util.Collection;

/**
 * Implements the write all operation for write behind
 * 
 * @author Abhishek Maheshwari
 */

public class WriteAllAsyncOperation implements BatchAsyncOperation {
  private final Collection<Element> elements;

  public WriteAllAsyncOperation(Collection<Element> elements) {
    this.elements = elements;
  }

  public void performBatchOperation(CacheWriter cacheWriter) {
    cacheWriter.writeAll(elements);
  }
}

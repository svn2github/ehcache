/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.operations;

import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;

import java.util.List;

/**
 * Implements the write all operation for write behind
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class WriteAllAsyncOperation implements BatchAsyncOperation {
  private final List<Element> elements;

  public WriteAllAsyncOperation(List<Element> elements) {
    this.elements = elements;
  }

  public void performBatchOperation(CacheWriter cacheWriter) {
    cacheWriter.writeAll(elements);
  }
}

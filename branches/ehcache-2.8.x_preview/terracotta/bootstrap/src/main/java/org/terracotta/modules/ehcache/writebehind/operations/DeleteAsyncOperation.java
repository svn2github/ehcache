/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.operations;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;

public class DeleteAsyncOperation implements SingleAsyncOperation {

  private final Object  keySnapshot;
  private final Element elementSnapshot;
  private final long    creationTime;

  public DeleteAsyncOperation(Object keySnapshot, Element elementSnapshot) {
    this.keySnapshot = keySnapshot;
    this.elementSnapshot = elementSnapshot;
    this.creationTime = System.currentTimeMillis();
  }

  @Override
  public void performSingleOperation(CacheWriter cacheWriter) {
    cacheWriter.delete(new CacheEntry(getKey(), getElement()));
  }

  @Override
  public Object getKey() {
    return keySnapshot;
  }

  @Override
  public Element getElement() {
    return elementSnapshot;
  }

  @Override
  public long getCreationTime() {
    return creationTime;
  }

  @Override
  public void throwAwayElement(CacheWriter cacheWriter, RuntimeException e) {
    cacheWriter.throwAway(elementSnapshot, SingleOperationType.DELETE, e);
  }

}

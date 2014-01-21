/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.operations;

import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;

public class WriteAsyncOperation implements SingleAsyncOperation {

  private static final long serialVersionUID = 1631728715404189659L;

  private final Element snapshot;
  private final long    creationTime;

  public WriteAsyncOperation(Element snapshot) {
    this(snapshot, System.currentTimeMillis());
  }

  public WriteAsyncOperation(Element snapshot, long creationTime) {
    this.snapshot = snapshot;
    this.creationTime = creationTime;
  }

  @Override
  public Element getElement() {
    return snapshot;
  }

  @Override
  public void performSingleOperation(CacheWriter cacheWriter) {
    cacheWriter.write(snapshot);
  }

  @Override
  public Object getKey() {
    return snapshot.getObjectKey();
  }

  @Override
  public long getCreationTime() {
    return creationTime;
  }

  @Override
  public void throwAwayElement(CacheWriter cacheWriter, RuntimeException e) {
    cacheWriter.throwAway(snapshot, SingleOperationType.WRITE, e);
  }

}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.operations;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;

import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.cache.serialization.SerializationStrategy;
import org.terracotta.modules.ehcache.writebehind.snapshots.ElementSnapshot;
import org.terracotta.modules.ehcache.writebehind.snapshots.KeySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the delete operation for write behind
 *
 * @author Geert Bevin
 * @version $Id$
 */
@InstrumentedClass
public class DeleteAsyncOperation implements SingleAsyncOperation {
  private final KeySnapshot keySnapshot;
  private final ElementSnapshot elementSnapshot;
  private final long creationTime;

  public DeleteAsyncOperation(KeySnapshot keySnapshot, ElementSnapshot elementSnapshot) {
    this.keySnapshot = keySnapshot;
    this.elementSnapshot = elementSnapshot;
    this.creationTime = System.currentTimeMillis();
  }

  public void performSingleOperation(CacheWriter cacheWriter, SerializationStrategy serializationStrategy) throws ClassNotFoundException, IOException {
    cacheWriter.delete(new CacheEntry(getKey(serializationStrategy), createElement(serializationStrategy)));
  }

  public Object getKey(SerializationStrategy serializationStrategy) throws IOException, ClassNotFoundException {
    return keySnapshot.getKey(serializationStrategy);
  }

  public Element createElement(SerializationStrategy serializationStrategy) throws ClassNotFoundException, IOException {
    if (null == elementSnapshot) {
      return null;
    }
    return elementSnapshot.createElement(serializationStrategy);
  }

  public BatchAsyncOperation createBatchOperation(List<SingleAsyncOperation> operations, SerializationStrategy serializationStrategy) throws ClassNotFoundException, IOException {
    final List<CacheEntry> entries = new ArrayList<CacheEntry>();
    for (SingleAsyncOperation operation : operations) {
      DeleteAsyncOperation deleteOperation = (DeleteAsyncOperation) operation;
      entries.add(new CacheEntry(deleteOperation.getKey(serializationStrategy), deleteOperation.createElement(serializationStrategy)));
    }
    return new DeleteAllAsyncOperation(entries);
  }

  public long getCreationTime() {
    return creationTime;
  }

  public void throwAwayElement(final CacheWriter cacheWriter, final SerializationStrategy serializationStrategy, final RuntimeException e) throws ClassNotFoundException, IOException {
    cacheWriter.throwAway(createElement(serializationStrategy), SingleOperationType.DELETE, e);
  }
}

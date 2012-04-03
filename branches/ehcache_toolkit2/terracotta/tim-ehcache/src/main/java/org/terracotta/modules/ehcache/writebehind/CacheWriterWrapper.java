/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.writer.CacheWriter;

import org.terracotta.async.ItemProcessor2;
import org.terracotta.cache.serialization.SerializationStrategy;
import org.terracotta.modules.ehcache.writebehind.operations.SingleAsyncOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@code ItemProcessor} that delegates the processing to a {@code CacheWriter} instance
 * <p/>
 * Instances of this class will be used when items are processed one by one. Note that {@code CacheWriter}
 * instances will not be shared across a Terracotta DSO cluster and are intended to be local on a node. They are tied to
 * an individual write behind queue. You're thus free to use local resources in an {@code CacheWriter}, like
 * database connections or file handles.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class CacheWriterWrapper implements ItemProcessor2<SingleAsyncOperation> {
  private final CacheWriter cacheWriter;
  private final SerializationStrategy serializationStrategy;

  /**
   * Creates a new item processor for a specific cache writer.
   *
   * @param cacheWriter           the cache writer for which the wrapper has to be created
   * @param serializationStrategy the strategy that should be used to serialize and deserialize, if needed
   */
  public CacheWriterWrapper(CacheWriter cacheWriter, SerializationStrategy serializationStrategy) {
    this.cacheWriter = cacheWriter;
    this.serializationStrategy = serializationStrategy;
  }

  public void process(SingleAsyncOperation operation) {
    try {
      operation.performSingleOperation(cacheWriter, serializationStrategy);
    } catch (Exception e) {
      throw new CacheException("Unexpected exception while processing write behind operation", e);
    }
  }

  public void process(Collection<SingleAsyncOperation> items) {

    // separate out the operations per type 
    final Map<Class, List<SingleAsyncOperation>> separatedItemsPerType = new HashMap<Class, List<SingleAsyncOperation>>();
    for (SingleAsyncOperation item : items) {
      List<SingleAsyncOperation> itemsPerType = separatedItemsPerType.get(item.getClass());
      if (null == itemsPerType) {
        itemsPerType = new ArrayList<SingleAsyncOperation>();
        separatedItemsPerType.put(item.getClass(), itemsPerType);
      }

      itemsPerType.add(item);
    }

    // execute the batch operations
    try {
      for (List<SingleAsyncOperation> itemsPerType : separatedItemsPerType.values()) {
        itemsPerType.get(0).createBatchOperation(itemsPerType, serializationStrategy).performBatchOperation(cacheWriter);
      }
    } catch (Exception e) {
      throw new CacheException("Unexpected exception while processing write behind operation", e);
    }
  }

  public void throwAway(final SingleAsyncOperation operation, RuntimeException runtimeException) {
    try {
      operation.throwAwayElement(cacheWriter, serializationStrategy, runtimeException);
    } catch (Exception e) {
      throw new CacheException("Unexpected exception while throwing away write behind operation", e);
    }
  }
}

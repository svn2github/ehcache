/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;

import org.terracotta.modules.ehcache.async.ItemProcessor;
import org.terracotta.modules.ehcache.writebehind.operations.BatchAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.DeleteAllAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.DeleteAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.SingleAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.WriteAllAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.WriteAsyncOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An implementation of {@code ItemProcessor} that delegates the processing to a {@code CacheWriter} instance
 * <p/>
 * Instances of this class will be used when items are processed one by one. Note that {@code CacheWriter} instances
 * will not be shared across a Terracotta DSO cluster and are intended to be local on a node. They are tied to an
 * individual write behind queue. You're thus free to use local resources in an {@code CacheWriter}, like database
 * connections or file handles.
 * 
 * @author Abhishek Maheshwari
 */
public class CacheWriterProcessor implements ItemProcessor<SingleAsyncOperation> {
  private final CacheWriter cacheWriter;

  /**
   * Creates a new item processor for a specific cache writer.
   * 
   * @param cacheWriter the cache writer for which the wrapper has to be created
   * @param serializationStrategy the strategy that should be used to serialize and deserialize, if needed
   */
  public CacheWriterProcessor(CacheWriter cacheWriter) {
    this.cacheWriter = cacheWriter;
  }

  @Override
  public void process(SingleAsyncOperation item) {
    try {
      item.performSingleOperation(cacheWriter);
    } catch (Exception e) {
      throw new CacheException("Unexpected exception while processing write behind operation", e);
    }

  }

  @Override
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
      for (Entry<Class, List<SingleAsyncOperation>> entry : separatedItemsPerType.entrySet()) {
        BatchAsyncOperation batch = createBatchOprForType(entry.getClass(), entry.getValue());
        batch.performBatchOperation(cacheWriter);
      }
    } catch (Exception e) {
      throw new CacheException("Unexpected exception while processing write behind operation", e);
    }
  }

  private BatchAsyncOperation createBatchOprForType(Class operationClass, Collection<SingleAsyncOperation> operations) {
    if (operationClass == WriteAsyncOperation.class) {
      final List<Element> elements = new ArrayList<Element>();
      for (SingleAsyncOperation operation : operations) {
        elements.add(operation.getElement());
      }
      return new WriteAllAsyncOperation(elements);
    }

    if (operationClass == DeleteAsyncOperation.class) {
      List<CacheEntry> entries = new ArrayList<CacheEntry>();
      for (SingleAsyncOperation operation : operations) {
        entries.add(new CacheEntry(operation.getKey(), operation.getElement()));
      }
      return new DeleteAllAsyncOperation(entries);
    }

    return null;
  }

  @Override
  public void throwAway(SingleAsyncOperation item, RuntimeException runtimeException) {
    try {
      item.throwAwayElement(cacheWriter, runtimeException);
    } catch (Exception e) {
      throw new CacheException("Unexpected exception while throwing away write behind operation", e);
    }
  }

}

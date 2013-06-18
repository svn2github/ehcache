/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.async.ItemProcessor;
import org.terracotta.modules.ehcache.writebehind.operations.BatchAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.DeleteAllAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.DeleteAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.SingleAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.WriteAllAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.WriteAsyncOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheWriterProcessor.class.getName());

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
    final List<SingleAsyncOperation> itemsPerType = new ArrayList<SingleAsyncOperation>();
    Class opClass = WriteAsyncOperation.class;
    for (SingleAsyncOperation item : items) {
      // keep adding items of same operationClass
      if (item.getClass() == opClass) {
        itemsPerType.add(item);
      } else {
        // execute the batch
        executeBatch(itemsPerType);
        // switch to new operationClass and add items in itemsPerType
        opClass = item.getClass();
        itemsPerType.clear();
        itemsPerType.add(item);
      }
    }
    // finally execute the last batch
    executeBatch(itemsPerType);
  }

  private void executeBatch(final List<SingleAsyncOperation> itemsPerType) {
    if (!itemsPerType.isEmpty()) {
      Class opClass = itemsPerType.get(0).getClass();
      try {
        BatchAsyncOperation batch = createBatchOprForType(opClass, itemsPerType);
        batch.performBatchOperation(cacheWriter);
      } catch (Exception e) {
        LOGGER.warn("error while processing batch write behind operation " + e);
        throw new CacheException("Unexpected exception while processing write behind operation " + e, e);
      }
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
    throw new RuntimeException("no batch operation created for " + operationClass.getName());
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

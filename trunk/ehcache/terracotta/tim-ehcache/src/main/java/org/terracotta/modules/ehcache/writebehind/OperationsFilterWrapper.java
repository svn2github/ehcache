/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.writer.writebehind.OperationConverter;
import net.sf.ehcache.writer.writebehind.OperationsFilter;
import net.sf.ehcache.writer.writebehind.operations.KeyBasedOperation;

import org.terracotta.async.QuarantinedItemsFilter;
import org.terracotta.cache.serialization.SerializationStrategy;
import org.terracotta.modules.ehcache.writebehind.operations.SingleAsyncOperation;

import java.util.List;

/**
 * An implementation of {@code QuarantinedItemsFilter} that delegates the processing to a {@code OperationsFilter} instance
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class OperationsFilterWrapper implements QuarantinedItemsFilter<SingleAsyncOperation> {
  private final OperationsFilter<KeyBasedOperation> delegate;
  private final SerializationStrategy serializationStrategy;

  public OperationsFilterWrapper(OperationsFilter<KeyBasedOperation> delegate, SerializationStrategy serializationStrategy) {
    this.delegate = delegate;
    this.serializationStrategy = serializationStrategy;
  }

  public void filter(List<SingleAsyncOperation> items) {
    delegate.filter(items, new OperationConverter<KeyBasedOperation>() {
      public KeyBasedOperation convert(Object source) {
        SingleAsyncOperation operation = (SingleAsyncOperation)source;
        try {
          return new KeyBasedOperationWrapper(operation.getKey(serializationStrategy), operation.getCreationTime());
        } catch (Exception e) {
          throw new CacheException(e);
        }
      }
    });
  }
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import org.terracotta.modules.ehcache.async.ItemsFilter;
import org.terracotta.modules.ehcache.writebehind.operations.SingleAsyncOperation;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.writer.writebehind.OperationConverter;
import net.sf.ehcache.writer.writebehind.OperationsFilter;
import net.sf.ehcache.writer.writebehind.operations.KeyBasedOperation;

import java.util.List;

public class OperationsFilterWrapper implements ItemsFilter<SingleAsyncOperation> {
  private final OperationsFilter<KeyBasedOperation> delegate;

  public OperationsFilterWrapper(OperationsFilter<KeyBasedOperation> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void filter(final List<SingleAsyncOperation> items) {
    delegate.filter(items, new OperationConverter<KeyBasedOperation>() {
      public KeyBasedOperation convert(Object source) {
        SingleAsyncOperation operation = (SingleAsyncOperation) source;
        try {
          return new KeyBasedOperationWrapper(operation.getKey(), operation.getCreationTime());
        } catch (Exception e) {
          throw new CacheException(e);
        }
      }
    });
  }
}

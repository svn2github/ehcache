/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
 * @version $Id: OperationsFilterWrapper.java 21899 2010-04-14 19:44:00Z gbevin $
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

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
package org.terracotta.modules.ehcache.writebehind.operations;

import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;

import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.cache.serialization.SerializationStrategy;
import org.terracotta.modules.ehcache.writebehind.snapshots.ElementSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the write operation for write behind
 *
 * @author Geert Bevin
 * @version $Id: WriteAsyncOperation.java 29067 2011-08-02 10:07:00Z alexsnaps $
 */
@InstrumentedClass
public class WriteAsyncOperation implements SingleAsyncOperation {
  private final ElementSnapshot snapshot;
  private final long creationTime;

  public WriteAsyncOperation(ElementSnapshot snapshot) {
    this.snapshot = snapshot;
    this.creationTime = System.currentTimeMillis();
  }

  public void performSingleOperation(CacheWriter cacheWriter, SerializationStrategy serializationStrategy) throws ClassNotFoundException, IOException {
    cacheWriter.write(createElement(serializationStrategy));
  }

  public Element createElement(SerializationStrategy serializationStrategy) throws ClassNotFoundException, IOException {
    return snapshot.createElement(serializationStrategy);
  }

  public BatchAsyncOperation createBatchOperation(List<SingleAsyncOperation> operations, SerializationStrategy serializationStrategy) throws ClassNotFoundException, IOException {
    final List<Element> elements = new ArrayList<Element>();
    for (SingleAsyncOperation operation : operations) {
      elements.add(((WriteAsyncOperation)operation).createElement(serializationStrategy));
    }
    return new WriteAllAsyncOperation(elements);
  }

  public Object getKey(SerializationStrategy serializationStrategy) throws IOException, ClassNotFoundException {
    return snapshot.getKey(serializationStrategy);
  }

  public long getCreationTime() {
    return creationTime;
  }

  public void throwAwayElement(final CacheWriter cacheWriter, final SerializationStrategy serializationStrategy, final RuntimeException e) throws ClassNotFoundException, IOException {
    cacheWriter.throwAway(createElement(serializationStrategy), SingleOperationType.WRITE, e);
  }
}
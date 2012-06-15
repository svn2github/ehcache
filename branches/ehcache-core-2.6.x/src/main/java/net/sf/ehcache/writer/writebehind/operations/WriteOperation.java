/**
 *  Copyright Terracotta, Inc.
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
package net.sf.ehcache.writer.writebehind.operations;

import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the write operation for write behind
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class WriteOperation implements SingleOperation {
    private final Element element;
    private final long creationTime;

    /**
     * Create a new write operation for a particular element
     *
     * @param element the element to write
     */
    public WriteOperation(Element element) {
        this(element, System.currentTimeMillis());
    }

    /**
     * Create a new write operation for a particular element and creation time
     *
     * @param element      the element to write
     * @param creationTime the creation time of the operation
     */
    public WriteOperation(Element element, long creationTime) {
        this.element = new Element(element.getObjectKey(), element.getObjectValue(), element.getVersion(),
                element.getCreationTime(), element.getLastAccessTime(), element.getHitCount(), false,
                element.getTimeToLive(), element.getTimeToIdle(), element.getLastUpdateTime());
        this.creationTime = creationTime;
    }

    /**
     * {@inheritDoc}
     */
    public void performSingleOperation(CacheWriter cacheWriter) {
        cacheWriter.write(element);
    }

    /**
     * {@inheritDoc}
     */
    public BatchOperation createBatchOperation(List<SingleOperation> operations) {
        final List<Element> elements = new ArrayList<Element>();
        for (KeyBasedOperation operation : operations) {
            elements.add(((WriteOperation) operation).element);
        }
        return new WriteAllOperation(elements);
    }

    /**
     * {@inheritDoc}
     */
    public Object getKey() {
        return element.getObjectKey();
    }

    /**
     * {@inheritDoc}
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Retrieves the element that will be used for this operation
     */
    public Element getElement() {
        return element;
    }

    /**
     * {@inheritDoc}
     */
    public SingleOperationType getType() {
        return SingleOperationType.WRITE;
    }

    /**
     * {@inheritDoc}
     */
    public void throwAway(final CacheWriter cacheWriter, final RuntimeException e) {
        cacheWriter.throwAway(element, SingleOperationType.WRITE, e);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
      int hash = (int) getCreationTime();
      hash = hash * 31 + getKey().hashCode();
      return hash;
    }

  /**
   * {@inheritDoc}
   */
  public boolean equals(Object other) {
    if (other instanceof WriteOperation) {
      return getCreationTime() == ((WriteOperation) other).getCreationTime() && getKey().equals(
              ((WriteOperation) other).getKey());
    } else {
      return false;
    }
  }
}

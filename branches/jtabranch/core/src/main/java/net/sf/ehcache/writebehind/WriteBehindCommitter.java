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
package net.sf.ehcache.writebehind;

/**
 * An interface for implementing the behavior to commit items in the write behind queue.
 * <p/>
 * Instances of this class will be used when items are processed one by one. Note that {@code WriteBehindCommitter}
 * instances will not be shared across a Terracotta DSO cluster and are intended to be local on a node. They are tied to
 * an individual write behind queue. You're thus free to use local resources in an {@code WriteBehindCommitter}, like
 * database connections or file handles.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public interface WriteBehindCommitter {
  /**
   * This method is called from within the write behind when enqueued items are processed one by one. When this method
   * finishes, the corresponding item will be definitively removed from the bucket. Note that while the item removal is
   * transactional and your implementation of the {@code commit} method can be done in a transactional fashion, they are
   * not transactional together. It is thus possible that your {@code WriteBehindCommitter} succeeds and that the node
   * goes down before the item is actually removed from the bucket. Your {@code WriteBehindCommitter} thus might have
   * to be implemented with some form of bookkeeping that is able to detect that a certain item has already been
   * processed. This can for example be a check on a unique item identifier that is allocated before starting the actual
   * processing of a new item.
   *
   * @param item the item that has to be processed
   */
  public void commit(Object item);
}
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

import org.terracotta.modules.async.AsyncCoordinatorOperations;

/**
 * A write behind implementation that relies on tim-async for its functionalities.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class AsyncWriteBehind implements WriteBehind {

  private final AsyncCoordinatorOperations async;

  /**
   * Instantiate a new instance of {@code AsyncWriteBehind} by providing the async coordinator instance that will
   * be used for the underlying behavior.
   *
   * @param async the async coordinator instance that will be used by the write behind queue
   */
  public AsyncWriteBehind(AsyncCoordinatorOperations async) {
    this.async = async;
  }

  /**
   * {@inheritDoc}
   */
  public void start(WriteBehindCommitter processor) {
    async.start(new WriteBehindCommitterWrapper(processor));
  }

  /**
   * {@inheritDoc}
   */
  public void add(Object item) {
    async.add(item);
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
    async.stop();
  }
}

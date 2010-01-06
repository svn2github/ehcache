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
 * An interface for write behind behavior.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public interface WriteBehind {
  /**
   * Start the write behind queue with a write behind committer
   * 
   * @param committer the committer instance that will be used to process each item that has been added to queue
   * @see #stop
   */
  void start(WriteBehindCommitter committer);

  /**
   * Add an item too the write behind queue.
   *
   * @param item the item that will be added to the write behind queue
   */
  void add(Object item);

  /**
   * Stop the coordinator and all the internal data structures.
   * <p/>
   * This stops as quickly as possible without losing any previously added items. However, no guarantees are made
   * towards the processing of these items. It's highly likely that items are still inside the internal data structures
   * and not processed.
   *
   * @see #start
   */
  void stop();
}
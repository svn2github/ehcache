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

import org.terracotta.modules.async.ItemProcessor;
import org.terracotta.modules.async.exceptions.ProcessingException;

/**
 * An implementation of {@code ItemProcessor} that delegates the processing to a {@code WriteBehindCommitter} instance
 * <p/>
 * Instances of this class will be used when items are processed one by one. Note that {@code WriteBehindCommitter}
 * instances will not be shared across a Terracotta DSO cluster and are intended to be local on a node. They are tied to
 * an individual write behind queue. You're thus free to use local resources in an {@code WriteBehindCommitter}, like
 * database connections or file handles.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class WriteBehindCommitterWrapper implements ItemProcessor {
  private final WriteBehindCommitter committer;

  /**
   * Creates a new item processor for a specific write behind committer.
   *
   * @param committer the write behind committer for which the wrapper has to be created
   */
  public WriteBehindCommitterWrapper(WriteBehindCommitter committer) {
    this.committer = committer;
  }

  /**
   * {@inheritDoc}
   */
  public void process(Object item) throws ProcessingException {
    committer.commit(item);
  }
}
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

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.writer.CacheWriter;

import java.util.List;

/**
 * Implements the delete all operation for write behind
 *
 * @author Geert Bevin
 * @version $Id: DeleteAllAsyncOperation.java 20734 2010-02-23 21:10:53Z gbevin $
 */
public class DeleteAllAsyncOperation implements BatchAsyncOperation {
  private final List<CacheEntry> entries;

  public DeleteAllAsyncOperation(List<CacheEntry> entries) {
    this.entries = entries;
  }

  public void performBatchOperation(CacheWriter cacheWriter) {
    cacheWriter.deleteAll(entries);
  }
}

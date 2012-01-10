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

import java.util.List;

/**
 * Implements the write all operation for write behind
 *
 * @author Geert Bevin
 * @version $Id: WriteAllAsyncOperation.java 20185 2010-01-22 01:19:05Z gbevin $
 */
public class WriteAllAsyncOperation implements BatchAsyncOperation {
  private final List<Element> elements;

  public WriteAllAsyncOperation(List<Element> elements) {
    this.elements = elements;
  }

  public void performBatchOperation(CacheWriter cacheWriter) {
    cacheWriter.writeAll(elements);
  }
}
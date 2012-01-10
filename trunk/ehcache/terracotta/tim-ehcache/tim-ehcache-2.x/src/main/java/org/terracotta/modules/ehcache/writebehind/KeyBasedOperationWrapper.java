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

import net.sf.ehcache.writer.writebehind.operations.KeyBasedOperation;

/**
 * An implementation of {@code KeyBasedOperation} that simply stores the values it has to return
 *
 * @author Geert Bevin
 * @version $Id: KeyBasedOperationWrapper.java 20308 2010-01-27 16:05:07Z gbevin $
 */
public class KeyBasedOperationWrapper implements KeyBasedOperation {
  private final Object key;
  private final long creationTime;

  public KeyBasedOperationWrapper(Object key, long creationTime) {
    this.key = key;
    this.creationTime = creationTime;
  }

  public Object getKey() {
    return key;
  }

  public long getCreationTime() {
    return creationTime;
  }
}

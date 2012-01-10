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
package org.terracotta.modules.ehcache.writebehind.snapshots;

import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.cache.serialization.SerializationStrategy;

import java.io.IOException;

/**
 * Abstract base class to for creating and restoring snapshots of Ehcache keys
 *
 * @author Geert Bevin
 * @version $Id: KeySnapshot.java 21899 2010-04-14 19:44:00Z gbevin $
 */
@InstrumentedClass
public abstract class KeySnapshot {
  public abstract Object getKey(SerializationStrategy strategy) throws IOException, ClassNotFoundException;
}
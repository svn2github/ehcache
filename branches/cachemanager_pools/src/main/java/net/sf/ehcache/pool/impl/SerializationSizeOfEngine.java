/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.SizeOfEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author Ludovic Orban
 */
public class SerializationSizeOfEngine implements SizeOfEngine {

  public long sizeOf(final Object key, final Object value, final Object container) {
      try {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos);

          oos.writeObject(key);
          int keySize = baos.size();
          baos.reset();

          oos.writeObject(value);
          int valueSize = baos.size();
          baos.reset();

          oos.writeObject(container);
          int containerSize = baos.size() - keySize - valueSize;

          oos.close();
          baos.close();

          return keySize + valueSize + containerSize;
      } catch (IOException e) {
          throw new RuntimeException("error sizing objects with serialization", e);
      }
  }
}

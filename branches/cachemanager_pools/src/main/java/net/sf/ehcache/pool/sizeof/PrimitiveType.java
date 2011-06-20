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

package net.sf.ehcache.pool.sizeof;

/**
* @author Alex Snaps
*/
enum PrimitiveType {

  BOOLEAN(boolean.class, 1),
  BYTE(byte.class, 1),
  CHAR(char.class, 2),
  SHORT(short.class, 2),
  INT(int.class, 4),
  FLOAT(float.class, 4),
  DOUBLE(double.class, 8),
  LONG(long.class, 8),
  CLASS(Class.class, 8) {
    @Override
    public int getSize() {
      return JvmInformation.POINTER_SIZE + JvmInformation.JAVA_POINTER_SIZE;
    }
  };

  private Class<?> type;
  private int size;


  PrimitiveType(Class<?> type, int size) {
    this.type = type;
    this.size = size;
  }

  public int getSize() {
    return size;
  }

  public Class<?> getType() {
    return type;
  }

  public static int getReferenceSize() {
    return JvmInformation.JAVA_POINTER_SIZE;
  }

  public static long getArraySize() {
    return CLASS.getSize() + INT.getSize();
  }

  public static PrimitiveType forType(final Class<?> type) {
    for (PrimitiveType primitiveType : values()) {
      if (primitiveType.getType() == type) {
        return primitiveType;
      }
    }
    return null;
  }
}

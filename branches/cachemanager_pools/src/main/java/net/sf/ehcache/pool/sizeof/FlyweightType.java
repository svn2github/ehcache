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
enum FlyweightType {

  //XXX These checks will end up interning all objects passed in - this could be fatal for Strings
  ENUM(Enum.class) { @Override
boolean isShared(final Object obj) { return true; } },
  STRING(String.class) { @Override
boolean isShared(final Object obj) { return obj == ((String)obj).intern(); } },
  BOOLEAN(Boolean.class) { @Override
boolean isShared(final Object obj) { return obj == Boolean.TRUE || obj == Boolean.FALSE; } },
  INTEGER(Integer.class) { @Override
boolean isShared(final Object obj) { return obj == Integer.valueOf((Integer)obj); } },
  SHORT(Short.class) { @Override
boolean isShared(final Object obj) { return obj == Short.valueOf((Short)obj); } },
  BYTE(Byte.class) { @Override
boolean isShared(final Object obj) { return obj == Byte.valueOf((Byte)obj); } },
  LONG(Long.class) { @Override
boolean isShared(final Object obj) { return obj == Long.valueOf((Long)obj); } },
  CHARACTER(Character.class) { @Override
boolean isShared(final Object obj) { return obj == Character.valueOf((Character)obj); } };

  private final Class<?> clazz;

  FlyweightType(final Class<?> clazz) {
    this.clazz = clazz;
  }

  abstract boolean isShared(Object obj);

  static boolean isFlyweight(Object object) {
    return object != null && getFlyweightType(object.getClass()) != null;
  }

  static FlyweightType getFlyweightType(final Class<?> aClazz) {
    for (FlyweightType flyweightType : values()) {
      if (flyweightType.clazz == aClazz) {
        return flyweightType;
      }
    }
    return null;
  }
}

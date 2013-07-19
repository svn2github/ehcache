/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package net.sf.ehcache.search.parser;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;

// TODO: Auto-generated Javadoc
public class MAttribute implements ModelElement<Attribute<?>> {

  /** The name. */
  private final String     name;

  /** Is this the key. */
  private final boolean    isKey;

  /** Is this the value. */
  private final boolean    isValue;

  /** The key. */
  public static MAttribute KEY   = new MAttribute("key", true, false);

  /** The value. */
  public static MAttribute VALUE = new MAttribute("value", false, true);

  /**
   * Instantiates a new attribute.
   * 
   * @param name the name
   * @param k the k
   * @param v the v
   */
  private MAttribute(String name, boolean k, boolean v) {
    this.name = name;
    isKey = k;
    isValue = v;
  }

  /**
   * Instantiates a new named attribute.
   * 
   * @param name the name
   */
  public MAttribute(String name) {
    this(name, false, false);
  }

  /**
   * Gets the name.
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Checks if is key.
   * 
   * @return true, if is key
   */
  public boolean isKey() {
    return isKey;
  }

  /**
   * Checks if is value.
   * 
   * @return true, if is value
   */
  public boolean isValue() {
    return isValue;
  }

  /**
   * As ehcache attribute string.
   * 
   * @return the string
   */
  public String asEhcacheAttributeString() {
    return asEhcacheObject().getAttributeName();
  }

  /**
   * Get this model attribute as an ehcache attribute.
   * 
   * @return the attribute
   */

  @SuppressWarnings("rawtypes")
  public Attribute<?> asEhcacheObject() {
    if (isKey()) {
      return Query.KEY;
    } else if (isValue()) {
      return Query.VALUE;
    } else {
      return new Attribute(name);
    }
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (isKey()) {
      return name;
    } else if (isValue()) {
      return name;
    } else {
      return "'" + name + "'";
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isKey ? 1231 : 1237);
    result = prime * result + (isValue ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MAttribute other = (MAttribute) obj;
    if (isKey != other.isKey) return false;
    if (isValue != other.isValue) return false;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    return true;
  
  }
  
}

/**
 *  Copyright Terracotta, Inc.
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
package net.sf.ehcache.search.parser;

// TODO: Auto-generated Javadoc
/**
 * Created with IntelliJ IDEA. User: cschanck Date: 3/27/13 Time: 4:53 PM To change this template use File | Settings |
 * File Templates.
 */
public class MTarget {

  /** The agg. */
  private final MAggregate agg;

  /** The attr. */
  private final MAttribute attr;

  /**
   * Instantiates a new model target of key/value.
   */
  public MTarget() {
    agg = null;
    attr = null;
  }

  /**
   * Instantiates a new model target from an aggregate.
   * 
   * @param agg the agg
   */
  public MTarget(MAggregate agg) {
    this.agg = agg;
    this.attr = null;
  }

  /**
   * Instantiates a new model target from attribute.
   * 
   * @param attr the attr
   */
  public MTarget(MAttribute attr) {
    this.attr = attr;
    this.agg = null;
  }

  /**
   * Gets the aggregate.
   * 
   * @return the aggregate
   */
  public MAggregate getAggregate() {
    return agg;
  }

  /**
   * Gets the attribute.
   * 
   * @return the attribute
   */
  public MAttribute getAttribute() {
    return attr;
  }

  /**
   * Checks if is attribute.
   * 
   * @return true, if is attribute
   */
  public boolean isAttribute() {
    return attr != null;
  }

  /**
   * Checks if is aggregate.
   * 
   * @return true, if is aggregate
   */
  public boolean isAggregate() {
    return agg != null;
  }

  /**
   * Checks if is star.
   * 
   * @return true, if is star
   */
  public boolean isStar() {
    return agg == null && attr == null;
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    if (agg != null) {
      return agg.toString();
    } else if (attr != null) {
      return attr.toString();
    } else {
      return "*";
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((agg == null) ? 0 : agg.hashCode());
    result = prime * result + ((attr == null) ? 0 : attr.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MTarget other = (MTarget) obj;
    if (agg == null) {
      if (other.agg != null) return false;
    } else if (!agg.equals(other.agg)) return false;
    if (attr == null) {
      if (other.attr != null) return false;
    } else if (!attr.equals(other.attr)) return false;
    return true;
  }
}

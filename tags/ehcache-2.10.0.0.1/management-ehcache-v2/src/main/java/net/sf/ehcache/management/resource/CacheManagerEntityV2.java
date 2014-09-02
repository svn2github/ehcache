/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.AbstractEntityV2;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * A {@link VersionedEntity} representing a cache manager resource from the management API.
 * </p>
 * 
 * @author brandony
 */
public class CacheManagerEntityV2 extends AbstractEntityV2 {
  private String name;

  private final Map<String, Object> attributes = new HashMap<String, Object>();

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * 
   * @return the cache manager's attributes
   */
  public Map<String, Object> getAttributes() {
    return attributes;
  }
}

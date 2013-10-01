/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.resource;

import java.util.HashMap;
import java.util.Map;


/**
 * <p>
 * An entity representing a cache resource from the management API.
 * </p>
 * 
 * @author brandony
 * 
 */
public class CacheEntity extends AbstractCacheEntity {
  private Map<String, Object> attributes = new HashMap<String, Object>();

  public Map<String, Object> getAttributes() {
    return attributes;
  }
}

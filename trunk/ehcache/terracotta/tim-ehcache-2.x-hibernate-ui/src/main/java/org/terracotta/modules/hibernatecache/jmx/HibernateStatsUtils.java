/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.hibernatecache.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public abstract class HibernateStatsUtils {
  public final static String STATS_BEAN_NAME_PREFIX = "net.sf.ehcache.hibernate:type=EhcacheHibernateStats";

  public static ObjectName getHibernateStatsBeanName(String name) throws MalformedObjectNameException {
    if (name == null) { throw new IllegalArgumentException("name"); }
    return new ObjectName(STATS_BEAN_NAME_PREFIX + ",name=" + name.replaceAll(":|=|\n", "."));
  }
}

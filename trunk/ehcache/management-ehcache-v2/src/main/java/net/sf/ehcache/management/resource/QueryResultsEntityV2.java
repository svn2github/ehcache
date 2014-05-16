/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.VersionedEntityV2;

/**
 * <p>
 * A {@link VersionedEntity} representing query results.
 * </p>
 * 
 * @author gkeim
 */
@SuppressWarnings("serial")
public class QueryResultsEntityV2 extends VersionedEntityV2 {
  private String     name;
  private String     agentId;
  private Object[][] data;

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
   * @return the agentId
   */
  @Override
  public String getAgentId() {
    return agentId;
  }

  /**
   * @param agentId to set
   */
  @Override
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  /**
   * @param data to set
   */
  public void setData(Object[][] data) {
    this.data = data;
  }

  /**
   * @return the query results data
   */
  public Object[][] getData() {
    return data;
  }
}

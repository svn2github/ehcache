/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.resource;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.terracotta.management.resource.VersionedEntity;
import org.w3c.dom.Element;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * <p>
 * A {@link VersionedEntity} representing a cache configuration resource from the management API.
 * </p>
 * 
 * @author brandony
 * 
 */
@XmlRootElement(name = "configuration")
public class CacheConfigEntity extends VersionedEntity {
  private String cacheName;
  private String cacheManagerName;
  private String agentId;

  private Element xml;

  @XmlAttribute
  public String getCacheManagerName() {
    return cacheManagerName;
  }

  public void setCacheManagerName(String cacheManagerName) {
    this.cacheManagerName = cacheManagerName;
  }

  @XmlAnyElement
  @JsonIgnore
  public Element getXml() {
    return xml;
  }

  public void setXml(Element xml) {
    this.xml = xml;
  }

  @XmlAttribute
  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  @Override
  @XmlAttribute
  public String getAgentId() {
    return agentId;
  }

  @Override
  public void setAgentId(String agentId) {
    this.agentId =  agentId;
  }
}

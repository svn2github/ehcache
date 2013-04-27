package net.sf.ehcache.management.resource;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.terracotta.management.resource.VersionedEntity;
import org.w3c.dom.Element;

/**
 * @author brandony
 */
@SuppressWarnings("serial")
@XmlRootElement(name = "configuration")
public class CacheManagerConfigEntity extends VersionedEntity {
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

  @Override
  public String getAgentId() {
    return agentId;
  }

  @Override
  @XmlAttribute
  public void setAgentId(String agentId) {
    this.agentId =  agentId;
  }
}

package net.sf.ehcache.management.resource;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.w3c.dom.Element;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * @author brandony
 */
public class CacheManagerConfigEntity {
  private String cacheManagerName;

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
}

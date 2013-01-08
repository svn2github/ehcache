package net.sf.ehcache.management.resource;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.w3c.dom.Element;

/**
 * @author brandony
 */
@SuppressWarnings("serial")
public class CacheManagerConfigEntity implements Serializable {
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

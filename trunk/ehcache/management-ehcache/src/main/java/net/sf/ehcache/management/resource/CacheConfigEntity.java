/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.resource;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.terracotta.management.resource.VersionedEntity;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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

  // include this only in JSON
  @JsonProperty
  private String xml;

  @XmlAttribute
  public String getCacheManagerName() {
    return cacheManagerName;
  }

  public void setCacheManagerName(String cacheManagerName) {
    this.cacheManagerName = cacheManagerName;
  }

  // include this only in XML
  @XmlAnyElement
  @JsonIgnore
  public Element getParsedXml() throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory domFact = DocumentBuilderFactory.newInstance();
    DocumentBuilder domBuilder = domFact.newDocumentBuilder();
    return domBuilder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
  }

  @XmlTransient
  public String getXml() {
    return xml;
  }

  public void setXml(String xml) {
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

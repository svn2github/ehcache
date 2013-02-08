package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.VersionedEntity;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author brandony
 */
@XmlRootElement(name = "configurations")
@XmlSeeAlso({CacheConfigEntity.class, CacheManagerConfigEntity.class})
public class ConfigContainerEntity<CONFIG extends CacheManagerConfigEntity> extends VersionedEntity {
  private Collection<CONFIG> configuration = new HashSet<CONFIG>();

  private String agentId;

  @XmlAttribute
  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public Collection<CONFIG> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Collection<CONFIG> configuration) {
    this.configuration = configuration;
  }

}

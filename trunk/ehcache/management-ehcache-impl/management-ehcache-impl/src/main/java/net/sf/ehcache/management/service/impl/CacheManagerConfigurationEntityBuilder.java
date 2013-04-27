/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.resource.CacheManagerConfigEntity;
import net.sf.ehcache.management.sampled.CacheManagerSampler;
import org.terracotta.management.resource.AgentEntity;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author brandony
 */
final class CacheManagerConfigurationEntityBuilder {
  private final List<CacheManagerSampler> cmSamplers = new ArrayList<CacheManagerSampler>();

  static CacheManagerConfigurationEntityBuilder createWith(CacheManagerSampler sampler) {
    return new CacheManagerConfigurationEntityBuilder(sampler);
  }

  private CacheManagerConfigurationEntityBuilder(CacheManagerSampler sampler) {
    addSampler(sampler);
  }

  CacheManagerConfigurationEntityBuilder add(CacheManagerSampler sampler) {
    addSampler(sampler);
    return this;
  }

  Collection<CacheManagerConfigEntity> build() {
    Collection<CacheManagerConfigEntity> cmces = new ArrayList<CacheManagerConfigEntity>(cmSamplers.size());

    DocumentBuilderFactory domFact = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder domBuilder = domFact.newDocumentBuilder();

      for (CacheManagerSampler sampler : cmSamplers) {
        CacheManagerConfigEntity cmce = new CacheManagerConfigEntity();
        cmce.setCacheManagerName(sampler.getName());
        cmce.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
        cmce.setVersion(this.getClass().getPackage().getImplementationVersion());
        String xml = sampler.generateActiveConfigDeclaration();

        Document config;
        try {
          config = domBuilder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException e) {
          throw new RuntimeException(String.format("Failed to parse cache manager configuration xml for \"%s\".", sampler.getName()), e);
        } catch (IOException e) {
          throw new RuntimeException(String.format("Failed to serialize cache manager configuration for \"%s\".", sampler.getName()), e);
        }

        cmce.setXml(config.getDocumentElement());
        cmces.add(cmce);
      }
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Failed to instantiate DocumentBuilder for parsing cache manager configurations", e);
    }

    return cmces;
  }

  private void addSampler(CacheManagerSampler sampler) {
    if (sampler == null) throw new IllegalArgumentException("sampler == null");
    cmSamplers.add(sampler);
  }
}

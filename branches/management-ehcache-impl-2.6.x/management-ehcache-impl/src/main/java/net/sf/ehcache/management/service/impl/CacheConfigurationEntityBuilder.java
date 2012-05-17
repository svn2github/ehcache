/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.resource.CacheConfigEntity;
import net.sf.ehcache.management.sampled.CacheManagerSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author brandony
 */
final class CacheConfigurationEntityBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(CacheManagerConfigurationEntityBuilder.class);

  private final Map<String, CacheManagerSampler> samplersByCName = new HashMap<String, CacheManagerSampler>();

  static CacheConfigurationEntityBuilder createWith(CacheManagerSampler sampler,
                                                           String cacheName) {
    return new CacheConfigurationEntityBuilder(sampler, cacheName);
  }

  private CacheConfigurationEntityBuilder(CacheManagerSampler sampler,
                                          String cacheName) {
    addSampler(sampler, cacheName);
  }

  CacheConfigurationEntityBuilder add(CacheManagerSampler sampler,
                                             String cacheName) {
    addSampler(sampler, cacheName);
    return this;
  }

  Collection<CacheConfigEntity> build() {
    Collection<CacheConfigEntity> cces = new ArrayList<CacheConfigEntity>();

    DocumentBuilderFactory domFact = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder domBuilder = domFact.newDocumentBuilder();

        for (Map.Entry<String, CacheManagerSampler> entry: samplersByCName.entrySet()) {
          CacheConfigEntity cce = new CacheConfigEntity();

          String cacheName = entry.getKey();
          cce.setCacheName(cacheName);

          CacheManagerSampler sampler = entry.getValue();
          cce.setCacheManagerName(sampler.getName());

          String xml = sampler.generateActiveConfigDeclaration(cacheName);

          Document config = null;
          try {
            config = domBuilder.parse(new InputSource(new StringReader(xml)));
          } catch (SAXException e) {
            LOG.error(String.format("Failed to parse cache configuration xml for \"%s\".", sampler.getName()),
                e);
          } catch (IOException e) {
            LOG.error(String.format("Failed to serialize manager configuration for \"%s\".", sampler.getName()),
                e);
          }

          if (config != null) {
            cce.setXml(config.getDocumentElement());
            cces.add(cce);
          }
        }

    } catch (ParserConfigurationException e) {
      LOG.error("Failed to instantiate DocumentBuilder for parsing cache configurations", e);
    }

    return cces;
  }

  private void addSampler(CacheManagerSampler sampler,
                          String cacheName) {
    if (sampler == null) throw new IllegalArgumentException("sampler == null");

    if (cacheName == null) throw new IllegalArgumentException("cacheName == null");

    if(!Arrays.asList(sampler.getCacheNames()).contains(cacheName)) {
      throw new IllegalArgumentException(String.format("Invalid cache name: %s.", cacheName));
    }

    samplersByCName.put(cacheName, sampler);
  }
}

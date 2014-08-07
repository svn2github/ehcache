/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.ClusteredInstanceFactoryAccessor;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.management.resource.CacheConfigEntity;
import net.sf.ehcache.management.resource.CacheEntity;
import net.sf.ehcache.management.resource.CacheManagerConfigEntity;
import net.sf.ehcache.management.resource.CacheManagerEntity;
import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;
import net.sf.ehcache.management.resource.QueryResultsEntity;
import net.sf.ehcache.management.sampled.CacheManagerSampler;
import net.sf.ehcache.management.sampled.CacheManagerSamplerImpl;
import net.sf.ehcache.management.sampled.CacheSampler;
import net.sf.ehcache.management.sampled.CacheSamplerImpl;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import net.sf.ehcache.management.service.SamplerRepositoryService;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.l1bridge.AbstractRemoteAgentEndpointImpl;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.l1bridge.RemoteCallException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.exceptions.ExceptionUtils;
import org.terracotta.management.resource.services.AgentService;
import org.terracotta.management.resource.services.LicenseService;
import org.terracotta.management.resource.services.Utils;

/**
 * A controller class registering new {@link CacheManager}.
 * </p>
 * An {@link EntityResourceFactory} implementation that interacts with the native Ehcache API.
 * </p>
 * A {@link CacheService} implementation that interacts with the native Ehcache API to manipulate {@link Cache}
 * objects.
 *
 * @author brandony
 */
public class DfltSamplerRepositoryService extends AbstractRemoteAgentEndpointImpl
    implements SamplerRepositoryService, EntityResourceFactory, CacheManagerService, CacheService, AgentService,
    DfltSamplerRepositoryServiceMBean {

  private static final Logger LOG = LoggerFactory.getLogger(DfltSamplerRepositoryService.class);

  public static final String MBEAN_NAME_PREFIX = "net.sf.ehcache:type=" + IDENTIFIER;
  public static final String AGENCY = "Ehcache";

  private final ThreadLocal<Boolean> tsaBridged = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };

  /**
   * Guarded By cacheManagerSamplerRepoLock
   */
  private final Map<String, SamplerRepoEntry> cacheManagerSamplerRepo = new HashMap<String, SamplerRepoEntry>();

  private final ReadWriteLock cacheManagerSamplerRepoLock = new ReentrantReadWriteLock();
  private volatile ObjectName objectName;
  protected final ManagementRESTServiceConfiguration configuration;

  public DfltSamplerRepositoryService(String clientUUID, ManagementRESTServiceConfiguration configuration) {
    this.configuration = configuration;
    if (clientUUID != null) {
      registerMBean(clientUUID);
    }
  }

  private static void enableNonStopFor(SamplerRepoEntry samplerRepoEntry, boolean enable) {
    ClusteredInstanceFactory clusteredInstanceFactory = ClusteredInstanceFactoryAccessor.getClusteredInstanceFactory(samplerRepoEntry.cacheManager);
    if (clusteredInstanceFactory != null) {
      clusteredInstanceFactory.enableNonStopForCurrentThread(enable);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final synchronized void registerMBean(String clientUUID) {
    if (clientUUID == null) {
      throw new NullPointerException("clientUUID cannot be null");
    }
    if (objectName == null) {
      ObjectName objectName;
      try {
        objectName = new ObjectName(MBEAN_NAME_PREFIX + ",node=" + clientUUID);
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        platformMBeanServer.registerMBean(this, objectName);
      } catch (InstanceAlreadyExistsException iaee) {
        // the MBean has already been registered -> mark its name as null so it won't be unregistered by this instance
        objectName = null;
      } catch (Exception e) {
        LOG.warn("Error registering SamplerRepositoryService MBean with UUID: " + clientUUID, e);
        objectName = null;
      }
      this.objectName = objectName;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    if (objectName != null) {
      try {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        platformMBeanServer.unregisterMBean(objectName);
      } catch (Exception e) {
        LOG.warn("Error unregistering SamplerRepositoryService MBean: " + objectName, e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] invoke(RemoteCallDescriptor remoteCallDescriptor) throws RemoteCallException {
    try {
      tsaBridged.set(true);
      return super.invoke(remoteCallDescriptor);
    } finally {
      tsaBridged.set(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getVersion() {
    return this.getClass().getPackage().getImplementationVersion();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAgency() {
    return AGENCY;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(CacheManager cacheManager) {
    String cmName = cacheManager.getName();
    cacheManagerSamplerRepoLock.writeLock().lock();

    try {
      if (!cacheManagerSamplerRepo.containsKey(cmName)) {
        SamplerRepoEntry entry = new SamplerRepoEntry(cacheManager);
        cacheManager.setCacheManagerEventListener(entry);
        cacheManagerSamplerRepo.put(cmName, entry);
      }
    } finally {
      cacheManagerSamplerRepoLock.writeLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void unregister(CacheManager cacheManager) {
    cacheManagerSamplerRepoLock.writeLock().lock();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.remove(cacheManager.getName());
      entry.destroy();
    } finally {
      cacheManagerSamplerRepoLock.writeLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasRegistered() {
    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      return !cacheManagerSamplerRepo.isEmpty();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<CacheManagerEntity> createCacheManagerEntities(Set<String> cacheManagerNames,
                                                                   Set<String> attributes) {
    CacheManagerEntityBuilder builder = null;
    Collection<CacheManagerEntity> entities;
    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (SamplerRepoEntry entry : cacheManagerSamplerRepo.values()) {
          builder = builder == null ? CacheManagerEntityBuilder.createWith(entry.getCacheManagerSampler()) : builder
              .add(entry.getCacheManagerSampler());
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null) {
            builder = builder == null ? CacheManagerEntityBuilder.createWith(entry.getCacheManagerSampler()) : builder
                .add(entry.getCacheManagerSampler());
          }
        }
      }
      if (builder == null) entities = new HashSet<CacheManagerEntity>(0);
      else entities = attributes == null ? builder.build() : builder.add(attributes).build();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    return entities;
  }

  @Override
  public Collection<CacheManagerConfigEntity> createCacheManagerConfigEntities(Set<String> cacheManagerNames) {
    CacheManagerConfigurationEntityBuilder builder = null;
    Collection<CacheManagerConfigEntity> entities;

    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (SamplerRepoEntry entry : cacheManagerSamplerRepo.values()) {
          builder = builder == null ? CacheManagerConfigurationEntityBuilder
              .createWith(entry.getCacheManagerSampler()) : builder.add(entry.getCacheManagerSampler());
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);

          if (entry != null) {
            builder = builder == null ? CacheManagerConfigurationEntityBuilder
                .createWith(entry.getCacheManagerSampler()) : builder.add(entry.getCacheManagerSampler());
          }
        }
      }
      if (builder == null) entities = new HashSet<CacheManagerConfigEntity>(0);
      else entities = builder.build();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    return entities;
  }

  @Override
  public Collection<CacheEntity> createCacheEntities(Set<String> cacheManagerNames,
                                                     Set<String> cacheNames,
                                                     Set<String> attributes) {
    CacheEntityBuilder builder = null;
    Collection<CacheEntity> entities;

    cacheManagerSamplerRepoLock.readLock().lock();

    List<SamplerRepoEntry> disabledSamplerRepoEntries = new ArrayList<SamplerRepoEntry>();

    try {
      if (cacheManagerNames == null) {
        for (Map.Entry<String, SamplerRepoEntry> entry : cacheManagerSamplerRepo.entrySet()) {
          enableNonStopFor(entry.getValue(), false);
          disabledSamplerRepoEntries.add(entry.getValue());
          for (CacheSampler sampler : entry.getValue().getComprehensiveCacheSamplers(cacheNames)) {
            builder = builder == null ? CacheEntityBuilder.createWith(sampler, entry.getKey()) : builder
                .add(sampler, entry.getKey());
          }
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null) {
            enableNonStopFor(entry, false);
            disabledSamplerRepoEntries.add(entry);
            for (CacheSampler sampler : entry.getComprehensiveCacheSamplers(cacheNames)) {
              builder = builder == null ? CacheEntityBuilder.createWith(sampler, cmName) : builder.add(sampler, cmName);
            }
          }
        }
      }
      if (builder == null) entities = new HashSet<CacheEntity>(0);
      else entities = attributes == null ? builder.build() : builder.add(attributes).build();
    } finally {
      for (SamplerRepoEntry samplerRepoEntry : disabledSamplerRepoEntries) {
        enableNonStopFor(samplerRepoEntry, true);
      }
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    return entities;
  }

  @Override
  public Collection<CacheConfigEntity> createCacheConfigEntities(Set<String> cacheManagerNames,
                                                                 Set<String> cacheNames) {
    CacheConfigurationEntityBuilder builder = null;
    Collection<CacheConfigEntity> entities;

    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (Map.Entry<String, SamplerRepoEntry> entry : cacheManagerSamplerRepo.entrySet()) {
          for (CacheSampler sampler : entry.getValue().getComprehensiveCacheSamplers(cacheNames)) {
            builder = builder == null ? CacheConfigurationEntityBuilder
                .createWith(entry.getValue().getCacheManagerSampler(), sampler.getCacheName()) : builder
                .add(entry.getValue().getCacheManagerSampler(), sampler.getCacheName());
          }
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null) {
            for (CacheSampler sampler : entry.getComprehensiveCacheSamplers(cacheNames)) {
              builder = builder == null ? CacheConfigurationEntityBuilder
                  .createWith(entry.getCacheManagerSampler(), sampler.getCacheName()) : builder
                  .add(entry.getCacheManagerSampler(), sampler.getCacheName());
            }
          }
        }
      }
      if (builder == null) entities = new HashSet<CacheConfigEntity>(0);
      else entities = builder.build();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    return entities;
  }

  @Override
  public Collection<CacheStatisticSampleEntity> createCacheStatisticSampleEntity(Set<String> cacheManagerNames,
                                                                                 Set<String> cacheNames,
                                                                                 Set<String> sampleNames) {
    CacheStatisticSampleEntityBuilder builder = CacheStatisticSampleEntityBuilder.createWith(sampleNames);

    cacheManagerSamplerRepoLock.readLock().lock();

    List<SamplerRepoEntry> disabledSamplerRepoEntries = new ArrayList<SamplerRepoEntry>();

    try {
      if (cacheManagerNames == null) {
        for (Map.Entry<String, SamplerRepoEntry> entry : cacheManagerSamplerRepo.entrySet()) {
          enableNonStopFor(entry.getValue(), false);
          disabledSamplerRepoEntries.add(entry.getValue());
          for (CacheSampler sampler : entry.getValue().getComprehensiveCacheSamplers(cacheNames)) {
            builder.add(sampler, entry.getKey());
          }
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null) {
            enableNonStopFor(entry, false);
            disabledSamplerRepoEntries.add(entry);
            for (CacheSampler sampler : entry.getComprehensiveCacheSamplers(cacheNames)) {
              builder.add(sampler, cmName);
            }
          }
        }
      }

      return builder.build();
    } finally {
      for (SamplerRepoEntry samplerRepoEntry : disabledSamplerRepoEntries) {
        enableNonStopFor(samplerRepoEntry, true);
      }
      cacheManagerSamplerRepoLock.readLock().unlock();
    }
  }


  @Override
  public void createOrUpdateCache(String cacheManagerName,
                                  String cacheName,
                                  CacheEntity resource) throws ServiceExecutionException {
    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
      if (entry != null) entry.updateCache(cacheName, resource);
      else throw new ServiceExecutionException("CacheManager not found !");
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

  }

  @Override
  public void clearCache(String cacheManagerName,
                         String cacheName) {
    cacheManagerSamplerRepoLock.readLock().lock();

    SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
    try {
      enableNonStopFor(entry, false);
      if (entry != null) entry.clearCache(cacheName);
    } finally {
      enableNonStopFor(entry, true);
      cacheManagerSamplerRepoLock.readLock().unlock();
    }
  }

  @Override
  public void updateCacheManager(String cacheManagerName,
                                 CacheManagerEntity resource) throws ServiceExecutionException {
    cacheManagerSamplerRepoLock.writeLock().lock();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
      if (entry != null) {
        CacheManagerSampler cms = entry.getCacheManagerSampler();
        checkForInvalidAttributes(cacheManagerName, resource);

        Object mbldsAttr = resource.getAttributes().get(SamplerRepoEntry.MAX_BYTES_LOCAL_DISK_STRING);
        if (mbldsAttr != null) cms.setMaxBytesLocalDiskAsString(mbldsAttr.toString());

        Object mblhsAttr = resource.getAttributes().get(SamplerRepoEntry.MAX_BYTES_LOCAL_HEAP_STRING);
        if (mblhsAttr != null) cms.setMaxBytesLocalHeapAsString(mblhsAttr.toString());
      } else {
        throw new ServiceExecutionException("CacheManager not found !");
      }
    } finally {
      cacheManagerSamplerRepoLock.writeLock().unlock();
    }
  }
  
  @Override
  public Collection<QueryResultsEntity> executeQuery(String cacheManagerName, String queryString) throws ServiceExecutionException {
    cacheManagerSamplerRepoLock.writeLock().lock();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
      if (entry != null) {
        try {
          enableNonStopFor(entry, false);
          CacheManagerSampler cms = entry.getCacheManagerSampler();
          return buildQueryResultsEntity(cacheManagerName, cms.executeQuery(queryString));
        } catch (Exception e) {
          Throwable t = ExceptionUtils.getRootCause(e);
          throw new ServiceExecutionException(t.getMessage());
        } finally {
          enableNonStopFor(entry, true);
        }
      } else {
        throw new ServiceExecutionException("CacheManager not found !");
      }
    } finally {
      cacheManagerSamplerRepoLock.writeLock().unlock();
    }
  }

  private Collection<QueryResultsEntity> buildQueryResultsEntity(String cacheManagerName, Object[][] data) {
    QueryResultsEntity qre = new QueryResultsEntity();

    qre.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
    qre.setVersion(this.getClass().getPackage().getImplementationVersion());
    qre.setName(cacheManagerName);
    qre.setData(data);

    return Collections.singleton(qre);
  }
  
  private void checkForInvalidAttributes(String cacheManagerName, CacheManagerEntity resource) throws ServiceExecutionException {
    boolean invalidAttributesFound =  false;
    StringBuilder errorMessage =  new StringBuilder("You are not allowed to update those attributes : ");
    if(resource.getName() != null && !resource.getName().equals(cacheManagerName)) {
      errorMessage.append("name ");
      invalidAttributesFound = true;
    }
    for(Map.Entry<String,Object> attribute : resource.getAttributes().entrySet()) {
      String key = attribute.getKey();
      if(!key.equals(SamplerRepoEntry.MAX_BYTES_LOCAL_DISK_STRING) &&
              !key.equals(SamplerRepoEntry.MAX_BYTES_LOCAL_HEAP_STRING) ) {
        errorMessage.append(key).append(" ");
        invalidAttributesFound = true;
      }
    }
    if (invalidAttributesFound) {
      errorMessage.append(". Only ")
                  .append(SamplerRepoEntry.MAX_BYTES_LOCAL_DISK_STRING)
                  .append(" and ")
                  .append(SamplerRepoEntry.MAX_BYTES_LOCAL_HEAP_STRING)
                  .append(" can be updated for a CacheManager.");
      throw new ServiceExecutionException(errorMessage.toString());
    }
  }

  private static void checkForInvalidAttributes(String cacheName, CacheEntity resource) throws ServiceExecutionException {
    boolean invalidAttributesFound =  false;
    StringBuilder errorMessage =  new StringBuilder("You are not allowed to update those attributes : ");
    if(resource.getName() != null && !resource.getName().equals(cacheName)) {
      errorMessage.append("name ");
      invalidAttributesFound = true;
    }
    Set<String> validAttributes =  new HashSet<String>();
    validAttributes.add(SamplerRepoEntry.ENABLED_ATTR);
    validAttributes.add(SamplerRepoEntry.BULK_LOAD_ENABLED);
    validAttributes.add(SamplerRepoEntry.MAX_ELEMENTS_ON_DISK);
    validAttributes.add(SamplerRepoEntry.LOGGING_ENABLED);
    validAttributes.add(SamplerRepoEntry.MAX_BYTES_LOCAL_DISK_STRING);
    validAttributes.add(SamplerRepoEntry.MAX_BYTES_LOCAL_HEAP_STRING);
    validAttributes.add(SamplerRepoEntry.MAX_ENTRIES_LOCAL_HEAP);
    validAttributes.add(SamplerRepoEntry.MAX_ENTRIES_IN_CACHE);
    validAttributes.add(SamplerRepoEntry.TIME_TO_IDLE_SEC);
    validAttributes.add(SamplerRepoEntry.TIME_TO_LIVE_SEC);

    for(Map.Entry<String,Object> attribute : resource.getAttributes().entrySet()) {
      String key = attribute.getKey();
      if(!validAttributes.contains(key) ) {
        errorMessage.append(key).append(" ");
        invalidAttributesFound = true;
      }
    }
    if (invalidAttributesFound) {
      errorMessage.append(". Only ");
      for (String validAttribute : validAttributes) {
        errorMessage.append(validAttribute).append(" ");
      }
      errorMessage.append("can be updated for a Cache.");
      throw new ServiceExecutionException(errorMessage.toString());
    }
  }

  @Override
  public Collection<AgentEntity> getAgents(Set<String> ids) throws ServiceExecutionException {
    if (ids.isEmpty()) {
      return Collections.singleton(buildAgentEntity());
    }

    Collection<AgentEntity> result = new ArrayList<AgentEntity>();

    for (String id : ids) {
      if (!id.equals(AgentEntity.EMBEDDED_AGENT_ID)) {
        throw new ServiceExecutionException("Unknown agent ID : " + id);
      }
      result.add(buildAgentEntity());
    }

    return result;
  }

  private AgentEntity buildAgentEntity() {
    AgentEntity e = new AgentEntity();
    e.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
    e.setVersion(this.getClass().getPackage().getImplementationVersion());
    e.setAgencyOf(AGENCY);

    StringBuilder sb = new StringBuilder();
    for (String cmName : cacheManagerSamplerRepo.keySet()) {
      sb.append(cmName).append(",");
    }
    if (sb.indexOf(",") > -1) {
      sb.deleteCharAt(sb.length() - 1);
    }

    e.getRootRepresentables().put("cacheManagerNames", sb.toString());
    return e;
  }

  @Override
  public Collection<AgentMetadataEntity> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    if (ids.isEmpty()) {
      return Collections.singleton(buildAgentMetadata());
    }

    Collection<AgentMetadataEntity> result = new ArrayList<AgentMetadataEntity>();

    for (String id : ids) {
      if (!id.equals(AgentEntity.EMBEDDED_AGENT_ID)) {
        throw new ServiceExecutionException("Unknown agent ID : " + id);
      }
      result.add(buildAgentMetadata());
    }

    return result;
  }

  private AgentMetadataEntity buildAgentMetadata() {
    AgentMetadataEntity ame = new AgentMetadataEntity();

    ame.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
    ame.setAgencyOf(AGENCY);
    ame.setVersion(this.getClass().getPackage().getImplementationVersion());
    ame.setAvailable(true);

    if (isTsaBridged()) {
      ame.setSecured(isTsaSecured());
      ame.setSslEnabled(isTsaSecured());
      ame.setNeedClientAuth(false);
    } else {
      ame.setSecured(Utils.trimToNull(configuration.getSecurityServiceLocation()) != null);
      ame.setSslEnabled(Utils.trimToNull(configuration.getSecurityServiceLocation()) != null);
      ame.setNeedClientAuth(configuration.isNeedClientAuth());
    }

    ame.setLicensed(ServiceLocator.locate(LicenseService.class).isLicensed());
    ame.setSampleHistorySize(configuration.getSampleHistorySize());
    ame.setSampleIntervalSeconds(configuration.getSampleIntervalSeconds());
    ame.setEnabled(configuration.isEnabled());

    return ame;
  }

  protected boolean isTsaBridged() {
    return tsaBridged.get();
  }

  protected boolean isTsaSecured() {
    return false;
  }


  /**
   * The repository entry class that is also a {@link CacheManagerEventListener}.
   */
  private static final class SamplerRepoEntry implements CacheManagerEventListener {
    private final static String ENABLED_ATTR = "Enabled";

    private final static String BULK_LOAD_ENABLED = "NodeBulkLoadEnabled";

    private final static String MAX_ELEMENTS_ON_DISK = "MaxElementsOnDisk";

    private final static String MAX_BYTES_LOCAL_DISK = "MaxBytesLocalDisk";

    private final static String MAX_BYTES_LOCAL_DISK_STRING = "MaxBytesLocalDiskAsString";

    private final static String MAX_BYTES_LOCAL_HEAP = "MaxBytesLocalHeap";

    private final static String MAX_BYTES_LOCAL_HEAP_STRING = "MaxBytesLocalHeapAsString";

    private final static String LOGGING_ENABLED = "LoggingEnabled";

    private final static String TIME_TO_IDLE_SEC = "TimeToIdleSeconds";

    private final static String TIME_TO_LIVE_SEC = "TimeToLiveSeconds";

    private final static String MAX_ENTRIES_LOCAL_HEAP = "MaxEntriesLocalHeap";

    private final static String MAX_ENTRIES_IN_CACHE = "MaxEntriesInCache";

    private CacheManager cacheManager;

    private CacheManagerSampler cacheManagerSampler;

    /**
     * Guarded by cacheSamplerMapLock
     */
    private Map<String, CacheSampler> cacheSamplersByName;

    private volatile Status status = Status.STATUS_UNINITIALISED;

    private final ReadWriteLock cacheSamplerMapLock = new ReentrantReadWriteLock();

    public SamplerRepoEntry(CacheManager cacheManager) {
      if (cacheManager == null) throw new IllegalArgumentException("cacheManager == null");

      this.cacheManagerSampler = new CacheManagerSamplerImpl(cacheManager);
      this.cacheManager = cacheManager;

      String[] cNames = cacheManager.getCacheNames();
      this.cacheSamplersByName = new HashMap<String, CacheSampler>(cNames.length);

      for (String cName : cNames) {
        cacheSamplersByName.put(cName, new CacheSamplerImpl(cacheManager.getEhcache(cName)));
      }
    }

    public CacheManagerSampler getCacheManagerSampler() {
      return cacheManagerSampler;
    }

    public Collection<CacheSampler> getComprehensiveCacheSamplers(Set<String> cacheSamplerNames) {
      Collection<CacheSampler> samplers = new HashSet<CacheSampler>();

      cacheSamplerMapLock.readLock().lock();
      try {
        if (cacheSamplerNames == null) {
          for (CacheSampler cs : cacheSamplersByName.values()) {
            samplers.add(cs);
          }
        } else {
          for (String cName : cacheSamplerNames) {
            CacheSampler cs = cacheSamplersByName.get(cName);
            if (cs != null) samplers.add(cs);
          }
        }
      } finally {
        cacheSamplerMapLock.readLock().unlock();
      }

      return samplers;
    }

    public void clearCache(String cacheSamplerName) {
      cacheSamplerMapLock.writeLock().lock();

      CacheSampler cs;
      try {
        cs = cacheSamplersByName.get(cacheSamplerName);
        if (cs != null) cs.removeAll();
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    public void updateCache(String cacheSamplerName,
                            CacheEntity cacheResource) throws ServiceExecutionException {
      cacheSamplerMapLock.writeLock().lock();

      CacheSampler cs;
      try {
        cs = cacheSamplersByName.get(cacheSamplerName);

        if (cs != null) {
          try {

            checkForInvalidAttributes(cacheSamplerName, cacheResource);

            Boolean enabledAttr = (Boolean) cacheResource.getAttributes().get(ENABLED_ATTR);
            if (enabledAttr != null) cs.setEnabled(enabledAttr);

            Boolean enabledBlkLoad = (Boolean) cacheResource.getAttributes().get(BULK_LOAD_ENABLED);
            if (enabledBlkLoad != null) cs.setNodeBulkLoadEnabled(enabledBlkLoad);

            Integer maxElementsOnDiskAttr = (Integer) cacheResource.getAttributes().get(MAX_ELEMENTS_ON_DISK);
            if (maxElementsOnDiskAttr != null) cs.setMaxElementsOnDisk(maxElementsOnDiskAttr);

            Boolean loggingEnabledAttr = (Boolean) cacheResource.getAttributes().get(LOGGING_ENABLED);
            if (loggingEnabledAttr != null) cs.setLoggingEnabled(loggingEnabledAttr);

            Object mbldsAttr = cacheResource.getAttributes().get(MAX_BYTES_LOCAL_DISK_STRING);
            if (mbldsAttr != null) cs.setMaxBytesLocalDiskAsString(mbldsAttr.toString());

            Object mblhsAttr = cacheResource.getAttributes().get(MAX_BYTES_LOCAL_HEAP_STRING);
            if (mblhsAttr != null) cs.setMaxBytesLocalHeapAsString(mblhsAttr.toString());

            Integer melhAttr = (Integer) cacheResource.getAttributes().get(MAX_ENTRIES_LOCAL_HEAP);
            if (melhAttr != null) cs.setMaxEntriesLocalHeap(melhAttr);

            Integer meicAttr = (Integer) cacheResource.getAttributes().get(MAX_ENTRIES_IN_CACHE);
            if (meicAttr != null) cs.setMaxEntriesInCache(meicAttr);

            Object ttiAttr = cacheResource.getAttributes().get(TIME_TO_IDLE_SEC);
            if (ttiAttr != null) cs.setTimeToIdleSeconds(Long.parseLong(ttiAttr.toString()));

            Object ttlAttr = cacheResource.getAttributes().get(TIME_TO_LIVE_SEC);
            if (ttlAttr != null) cs.setTimeToLiveSeconds(Long.parseLong(ttlAttr.toString()));
          } catch (RuntimeException e) {
            throw new ServiceExecutionException(e);
          }

        } else {
          throw new ServiceExecutionException("Cache not found !");
        }

      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws CacheException {
      status = Status.STATUS_ALIVE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status getStatus() {
      return status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() throws CacheException {
      cacheSamplerMapLock.writeLock().lock();

      try {
        cacheSamplersByName.clear();
        cacheSamplersByName = null;
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }

      status = Status.STATUS_SHUTDOWN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyCacheAdded(String cacheName) {
      cacheSamplerMapLock.writeLock().lock();
      try {
        Cache c = cacheManager.getCache(cacheName);

        if (c != null) {
          cacheSamplersByName.put(cacheName, new CacheSamplerImpl(c));
        }
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyCacheRemoved(String cacheName) {
      cacheSamplerMapLock.writeLock().lock();

      try {
        cacheSamplersByName.remove(cacheName);
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    public void destroy() {
      cacheManagerSampler = null;
      cacheManager = null;
    }
  }
}

/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.terracotta.upgradability;

import com.terracotta.entity.ehcache.ClusteredCacheManager;
import com.terracotta.entity.ehcache.ToolkitBackedClusteredCache;

import java.io.Serializable;
import java.util.Properties;
import java.util.Set;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.constructs.refreshahead.RefreshAheadCache;
import net.sf.ehcache.constructs.refreshahead.RefreshAheadCacheConfiguration;
import net.sf.ehcache.constructs.scheduledrefresh.ScheduledRefreshCacheExtension;
import net.sf.ehcache.constructs.scheduledrefresh.ScheduledRefreshConfiguration;
import net.sf.ehcache.transaction.Decision;
import net.sf.ehcache.writer.AbstractCacheWriter;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterFactory;
import org.junit.Test;
import org.quartz.JobKey;
import org.terracotta.modules.ehcache.async.AsyncConfig;
import org.terracotta.modules.ehcache.transaction.SerializedReadCommittedClusteredSoftLock;
import org.terracotta.quartz.collections.TimeTrigger;
import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.store.ConfigFieldsInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;

import static net.sf.ehcache.config.CacheConfiguration.TransactionalMode.LOCAL;
import static net.sf.ehcache.config.CacheConfiguration.TransactionalMode.XA;
import static net.sf.ehcache.config.CacheConfiguration.TransactionalMode.XA_STRICT;
import static net.sf.ehcache.config.CacheWriterConfiguration.WriteMode.WRITE_BEHIND;
import static net.sf.ehcache.config.MemoryUnit.MEGABYTES;
import static net.sf.ehcache.config.TerracottaConfiguration.Consistency.STRONG;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.EVENTUAL;
import static org.terracotta.upgradability.interaction.MockToolkitFactoryService.allowNonPersistentInteractions;
import static org.terracotta.upgradability.interaction.MockToolkitFactoryService.mockToolkitFor;

/**
 *
 * @author cdennis
 */
public class InteractionTest {
  
  @Test
  public void testBasicCache() {
    final String managerName = "foo";
    final String cacheName = "bar";
    ToolkitInternal toolkit = mockToolkitFor("testBasicCache");
    
    CacheManager manager = new CacheManager(new Configuration().name(managerName).terracotta(new TerracottaClientConfiguration().url("testBasicCache")));
    try {
      Cache cache = new Cache(new CacheConfiguration().name(cacheName).maxBytesLocalHeap(4, MEGABYTES).terracotta(new TerracottaConfiguration()));
      manager.addCache(cache);
      
      cache.put(new Element("foo", "bar"));
    } finally {
      manager.shutdown();
    }
    
    verify(toolkit, atLeast(1)).getMap("com.terracotta.entity.ehcache.ClusteredCacheManager", String.class, ClusteredCacheManager.class);
    verify(toolkit, atLeast(1)).getMap("__entity_cache_root@" + managerName, String.class, ToolkitBackedClusteredCache.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|__tc_clustered-ehcache|configMap", String.class, Serializable.class);

    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName), refEq(new ToolkitCacheConfigBuilder()
            .maxTTLSeconds(0).maxTTISeconds(0)
            .consistency(EVENTUAL).concurrency(256)
            .maxBytesLocalOffheap(0).maxBytesLocalHeap(MEGABYTES.toBytes(4L)).maxCountLocalHeap(0)
            .evictionEnabled(true).localCacheEnabled(true).pinnedInLocalMemory(false)
            .offheapEnabled(false).copyOnReadEnabled(false).compressionEnabled(false)
            .configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME, managerName)
            .build()
    ), eq(Serializable.class));
    
    verify(toolkit).getProperties();
    
    verify(toolkit).shutdown();
    allowNonPersistentInteractions(toolkit);
    verifyNoMoreInteractions(toolkit);
  }
  
  @Test
  public void testLocalTransactionalCache() {
    final String managerName = "foo";
    final String cacheName = "bar";
    ToolkitInternal toolkit = mockToolkitFor("testLocalTransactionalCache");
    
    CacheManager manager = new CacheManager(new Configuration().name(managerName).terracotta(new TerracottaClientConfiguration().url("testLocalTransactionalCache")));
    try {
      Cache cache = new Cache(new CacheConfiguration().name(cacheName).maxBytesLocalHeap(4, MEGABYTES)
              .terracotta(new TerracottaConfiguration().consistency(STRONG)).transactionalMode(LOCAL));
      manager.addCache(cache);
      
      manager.getTransactionController().begin();
      try {
        cache.put(new Element("foo", "bar"));
      } finally {
        manager.getTransactionController().commit();
      }
    } finally {
      manager.shutdown();
    }
    
    verify(toolkit, atLeast(1)).getMap("com.terracotta.entity.ehcache.ClusteredCacheManager", String.class, ClusteredCacheManager.class);
    verify(toolkit, atLeast(1)).getMap("__entity_cache_root@" + managerName, String.class, ToolkitBackedClusteredCache.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|__tc_clustered-ehcache|configMap", String.class, Serializable.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|newSoftLocks", SerializedReadCommittedClusteredSoftLock.class, Integer.class);

    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName), refEq(new ToolkitCacheConfigBuilder()
            .maxTTLSeconds(0).maxTTISeconds(0)
            .consistency(Consistency.STRONG).concurrency(256)
            .maxBytesLocalOffheap(0).maxBytesLocalHeap(MEGABYTES.toBytes(4L)).maxCountLocalHeap(0)
            .evictionEnabled(true).localCacheEnabled(true).pinnedInLocalMemory(false)
            .offheapEnabled(false).copyOnReadEnabled(true).compressionEnabled(false)
            .configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME, managerName)
            .build()
    ), eq(Serializable.class));
    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|softLocks"), 
            refEq(new ToolkitCacheConfigBuilder().consistency(Consistency.STRONG).build()),
            eq(SerializedReadCommittedClusteredSoftLock.class));

    verify(toolkit).getCache(eq(managerName + "|__tc_clustered-ehcache|txnsDecision"), 
            refEq(new ToolkitCacheConfigBuilder().consistency(Consistency.SYNCHRONOUS_STRONG).build()),
            eq(Decision.class));
    
    verify(toolkit).getProperties();
    
    verify(toolkit).shutdown();
    allowNonPersistentInteractions(toolkit);
    verifyNoMoreInteractions(toolkit);
  }
  
  @Test
  public void testXaTransactionalCache() throws Exception {
    final String managerName = "foo";
    final String cacheName = "bar";
    ToolkitInternal toolkit = mockToolkitFor("testXaTransactionalCache");
    
    TransactionManagerServices.getConfiguration().setJournal("null").setGracefulShutdownInterval(0).setBackgroundRecoveryIntervalSeconds(1);
    BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
    try {
      CacheManager manager = new CacheManager(new Configuration().name(managerName).terracotta(new TerracottaClientConfiguration().url("testXaTransactionalCache")));
      try {
        Cache cache = new Cache(new CacheConfiguration().name(cacheName).maxBytesLocalHeap(4, MEGABYTES)
                .terracotta(new TerracottaConfiguration().consistency(STRONG)).transactionalMode(XA));
        manager.addCache(cache);

        tm.begin();
        try {
          cache.put(new Element("foo", "bar"));
        } finally {
          tm.commit();
        }
      } finally {
        manager.shutdown();
      }
    } finally {
      tm.shutdown();
    }
    
    verify(toolkit, atLeast(1)).getMap("com.terracotta.entity.ehcache.ClusteredCacheManager", String.class, ClusteredCacheManager.class);
    verify(toolkit, atLeast(1)).getMap("__entity_cache_root@" + managerName, String.class, ToolkitBackedClusteredCache.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|__tc_clustered-ehcache|configMap", String.class, Serializable.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|newSoftLocks", SerializedReadCommittedClusteredSoftLock.class, Integer.class);

    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName), refEq(new ToolkitCacheConfigBuilder()
            .maxTTLSeconds(0).maxTTISeconds(0)
            .consistency(Consistency.STRONG).concurrency(256)
            .maxBytesLocalOffheap(0).maxBytesLocalHeap(MEGABYTES.toBytes(4L)).maxCountLocalHeap(0)
            .evictionEnabled(true).localCacheEnabled(true).pinnedInLocalMemory(false)
            .offheapEnabled(false).copyOnReadEnabled(true).compressionEnabled(false)
            .configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME, managerName)
            .build()
    ), eq(Serializable.class));
    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|softLocks"), 
            refEq(new ToolkitCacheConfigBuilder().consistency(Consistency.STRONG).build()),
            eq(SerializedReadCommittedClusteredSoftLock.class));

    verify(toolkit).getCache(eq(managerName + "|__tc_clustered-ehcache|txnsDecision"), 
            refEq(new ToolkitCacheConfigBuilder().consistency(Consistency.SYNCHRONOUS_STRONG).build()),
            eq(Decision.class));
    
    verify(toolkit).getProperties();
    
    verify(toolkit).shutdown();
    allowNonPersistentInteractions(toolkit);
    verifyNoMoreInteractions(toolkit);
  }
  
  @Test
  public void testXaStrictTransactionalCache() throws Exception {
    final String managerName = "foo";
    final String cacheName = "bar";
    ToolkitInternal toolkit = mockToolkitFor("testXaStrictTransactionalCache");
    
    TransactionManagerServices.getConfiguration().setJournal("null").setGracefulShutdownInterval(0).setBackgroundRecoveryIntervalSeconds(1);
    BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
    try {
      CacheManager manager = new CacheManager(new Configuration().name(managerName).terracotta(new TerracottaClientConfiguration().url("testXaStrictTransactionalCache")));
      try {
        Cache cache = new Cache(new CacheConfiguration().name(cacheName).maxBytesLocalHeap(4, MEGABYTES)
                .terracotta(new TerracottaConfiguration().consistency(STRONG)).transactionalMode(XA_STRICT));
        manager.addCache(cache);

        tm.begin();
        try {
          cache.put(new Element("foo", "bar"));
        } finally {
          tm.commit();
        }
      } finally {
        manager.shutdown();
      }
    } finally {
      tm.shutdown();
    }
    
    verify(toolkit, atLeast(1)).getMap("com.terracotta.entity.ehcache.ClusteredCacheManager", String.class, ClusteredCacheManager.class);
    verify(toolkit, atLeast(1)).getMap("__entity_cache_root@" + managerName, String.class, ToolkitBackedClusteredCache.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|__tc_clustered-ehcache|configMap", String.class, Serializable.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|newSoftLocks", SerializedReadCommittedClusteredSoftLock.class, Integer.class);

    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName), refEq(new ToolkitCacheConfigBuilder()
            .maxTTLSeconds(0).maxTTISeconds(0)
            .consistency(Consistency.STRONG).concurrency(256)
            .maxBytesLocalOffheap(0).maxBytesLocalHeap(MEGABYTES.toBytes(4L)).maxCountLocalHeap(0)
            .evictionEnabled(true).localCacheEnabled(true).pinnedInLocalMemory(false)
            .offheapEnabled(false).copyOnReadEnabled(true).compressionEnabled(false)
            .configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME, managerName)
            .build()
    ), eq(Serializable.class));
    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|softLocks"), 
            refEq(new ToolkitCacheConfigBuilder().consistency(Consistency.STRONG).build()),
            eq(SerializedReadCommittedClusteredSoftLock.class));

    verify(toolkit).getCache(eq(managerName + "|__tc_clustered-ehcache|txnsDecision"), 
            refEq(new ToolkitCacheConfigBuilder().consistency(Consistency.SYNCHRONOUS_STRONG).build()),
            eq(Decision.class));
    
    verify(toolkit).getProperties();
    
    verify(toolkit).shutdown();
    allowNonPersistentInteractions(toolkit);
    verifyNoMoreInteractions(toolkit);
  }
  
  @Test
  public void testWriteBehindCache() {
    final String managerName = "foo";
    final String cacheName = "bar";
    ToolkitInternal toolkit = mockToolkitFor("testWriteBehindCache");
    
    CacheManager manager = new CacheManager(new Configuration().name(managerName).terracotta(new TerracottaClientConfiguration().url("testWriteBehindCache")));
    try {
      Cache cache = new Cache(new CacheConfiguration().name(cacheName).maxBytesLocalHeap(4, MEGABYTES).terracotta(new TerracottaConfiguration())
              .cacheWriter(new CacheWriterConfiguration().writeMode(WRITE_BEHIND)
                      .cacheWriterFactory(new CacheWriterConfiguration.CacheWriterFactoryConfiguration().className(NullCacheWriterFactory.class.getName()))));
      manager.addCache(cache);
      
      cache.put(new Element("foo", "bar"));
    } finally {
      manager.shutdown();
    }
    
    verify(toolkit, atLeast(1)).getMap("com.terracotta.entity.ehcache.ClusteredCacheManager", String.class, ClusteredCacheManager.class);
    verify(toolkit, atLeast(1)).getMap("__entity_cache_root@" + managerName, String.class, ToolkitBackedClusteredCache.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|__tc_clustered-ehcache|configMap", String.class, Serializable.class);
    verify(toolkit, atLeast(1)).getMap("asyncConfigMap", String.class, AsyncConfig.class);
    verify(toolkit, atLeast(1)).getMap(managerName + "|" + cacheName, String.class, Set.class);

    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName), refEq(new ToolkitCacheConfigBuilder()
            .maxTTLSeconds(0).maxTTISeconds(0)
            .consistency(EVENTUAL).concurrency(256)
            .maxBytesLocalOffheap(0).maxBytesLocalHeap(MEGABYTES.toBytes(4L)).maxCountLocalHeap(0)
            .evictionEnabled(true).localCacheEnabled(true).pinnedInLocalMemory(false)
            .offheapEnabled(false).copyOnReadEnabled(false).compressionEnabled(false)
            .configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME, managerName)
            .build()
    ), eq(Serializable.class));
    
    verify(toolkit).getList("foo|bar|LocalClusterNode|0", null);
    
    verify(toolkit, atLeast(1)).getProperties();
    
    verify(toolkit).shutdown();
    allowNonPersistentInteractions(toolkit);
    verifyNoMoreInteractions(toolkit);
  }
  
  @Test
  public void testRefreshAheadCache() {
    final String managerName = "foo";
    final String cacheName = "bar";
    ToolkitInternal toolkit = mockToolkitFor("testRefreshAheadCache");
    
    CacheManager manager = new CacheManager(new Configuration().name(managerName).terracotta(new TerracottaClientConfiguration().url("testRefreshAheadCache")));
    try {
      Cache cache = new Cache(new CacheConfiguration().name(cacheName).maxBytesLocalHeap(4, MEGABYTES).terracotta(new TerracottaConfiguration()));
      manager.addCache(cache);
      
      Ehcache refreshing = new RefreshAheadCache(cache, new RefreshAheadCacheConfiguration().maximumRefreshBacklogItems(1).build());
      refreshing.put(new Element("foo", "bar"));
    } finally {
      manager.shutdown();
    }
    
    verify(toolkit, atLeast(1)).getMap("com.terracotta.entity.ehcache.ClusteredCacheManager", String.class, ClusteredCacheManager.class);
    verify(toolkit, atLeast(1)).getMap("__entity_cache_root@" + managerName, String.class, ToolkitBackedClusteredCache.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|__tc_clustered-ehcache|configMap", String.class, Serializable.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "_net.sf.ehcache.constructs.refreshahead.RefreshAheadCache_refreshAheadSupport|__tc_clustered-ehcache|configMap", String.class, Serializable.class);
    
    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName), refEq(new ToolkitCacheConfigBuilder()
            .maxTTLSeconds(0).maxTTISeconds(0)
            .consistency(EVENTUAL).concurrency(256)
            .maxBytesLocalOffheap(0).maxBytesLocalHeap(MEGABYTES.toBytes(4L)).maxCountLocalHeap(0)
            .evictionEnabled(true).localCacheEnabled(true).pinnedInLocalMemory(false)
            .offheapEnabled(false).copyOnReadEnabled(false).compressionEnabled(false)
            .configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME, managerName)
            .build()
    ), eq(Serializable.class));
    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "_net.sf.ehcache.constructs.refreshahead.RefreshAheadCache_refreshAheadSupport"), refEq(new ToolkitCacheConfigBuilder()
            .maxTTLSeconds(600).maxTTISeconds(0)
            .consistency(Consistency.STRONG).concurrency(256)
            .maxBytesLocalOffheap(0).maxBytesLocalHeap(0).maxCountLocalHeap(200)
            .evictionEnabled(true).localCacheEnabled(true).pinnedInLocalMemory(false)
            .offheapEnabled(false).copyOnReadEnabled(false).compressionEnabled(false)
            .configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME, managerName)
            .build()
    ), eq(Serializable.class));
    
    verify(toolkit, atLeast(1)).getProperties();
    
    verify(toolkit).shutdown();
    allowNonPersistentInteractions(toolkit);
    verifyNoMoreInteractions(toolkit);
  }

  @Test
  public void testScheduledRefreshCache() {
    final String managerName = "foo";
    final String cacheName = "bar";
    ToolkitInternal toolkit = mockToolkitFor("testScheduledRefreshCache");
    
    CacheManager manager = new CacheManager(new Configuration().name(managerName).terracotta(new TerracottaClientConfiguration().url("testScheduledRefreshCache")));
    try {
      Cache cache = new Cache(new CacheConfiguration().name(cacheName).maxBytesLocalHeap(4, MEGABYTES).terracotta(new TerracottaConfiguration()));
      cache.registerCacheExtension(new ScheduledRefreshCacheExtension(
              new ScheduledRefreshConfiguration().cronExpression("0 0 0 1 JAN ? 2099")
                      .terracottaConfigUrl("testScheduledRefreshCache").build(), cache));
      manager.addCache(cache);

      cache.put(new Element("foo", "bar"));
    } finally {
      manager.shutdown();
    }
    
    verify(toolkit, atLeast(1)).getMap("com.terracotta.entity.ehcache.ClusteredCacheManager", String.class, ClusteredCacheManager.class);
    verify(toolkit, atLeast(1)).getMap("__entity_cache_root@" + managerName, String.class, ToolkitBackedClusteredCache.class);
    verify(toolkit, atLeast(1)).getMap("__tc_clustered-ehcache|" + managerName + "|" + cacheName + "|__tc_clustered-ehcache|configMap", String.class, Serializable.class);
    
    verify(toolkit).getCache(eq("__tc_clustered-ehcache|" + managerName + "|" + cacheName), refEq(new ToolkitCacheConfigBuilder()
            .maxTTLSeconds(0).maxTTISeconds(0)
            .consistency(EVENTUAL).concurrency(256)
            .maxBytesLocalOffheap(0).maxBytesLocalHeap(MEGABYTES.toBytes(4L)).maxCountLocalHeap(0)
            .evictionEnabled(true).localCacheEnabled(true).pinnedInLocalMemory(false)
            .offheapEnabled(false).copyOnReadEnabled(false).compressionEnabled(false)
            .configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME, managerName)
            .build()
    ), eq(Serializable.class));
    
    verify(toolkit).getStore(eq("_tc_quartz_jobs|scheduledRefresh_" + managerName + "_" + cacheName),
            refEq(new ToolkitStoreConfigBuilder().consistency(ToolkitConfigFields.Consistency.STRONG).concurrency(1).build()),
            isNull(Class.class));
    verify(toolkit).getStore(eq("_tc_quartz_triggers|scheduledRefresh_" + managerName + "_" + cacheName),
            refEq(new ToolkitStoreConfigBuilder().consistency(ToolkitConfigFields.Consistency.STRONG).concurrency(1).build()),
            isNull(Class.class));
    verify(toolkit).getStore(eq("_tc_quartz_fired_trigger|scheduledRefresh_" + managerName + "_" + cacheName),
            refEq(new ToolkitStoreConfigBuilder().consistency(ToolkitConfigFields.Consistency.STRONG).concurrency(1).build()),
            isNull(Class.class));
    verify(toolkit).getStore(eq("_tc_quartz_calendar_wrapper|scheduledRefresh_" + managerName + "_" + cacheName),
            refEq(new ToolkitStoreConfigBuilder().consistency(ToolkitConfigFields.Consistency.STRONG).concurrency(1).build()),
            isNull(Class.class));
    
    verify(toolkit).getSet("_tc_quartz_grp_names|scheduledRefresh_" + managerName + "_" + cacheName, String.class);
    verify(toolkit).getSet("_tc_quartz_grp_paused_names|scheduledRefresh_" + managerName + "_" + cacheName, String.class);
    verify(toolkit).getSet("_tc_quartz_blocked_jobs|scheduledRefresh_" + managerName + "_" + cacheName, JobKey.class);
    verify(toolkit).getSet("_tc_quartz_grp_names_triggers|scheduledRefresh_" + managerName + "_" + cacheName, String.class);
    verify(toolkit).getSet("_tc_quartz_grp_paused_trogger_names|scheduledRefresh_" + managerName + "_" + cacheName, String.class);
    verify(toolkit).getSet("_tc_quartz_grp_jobs_scheduledRefresh_" + managerName + "_" + cacheName + "_grp|scheduledRefresh_" + managerName + "_" + cacheName, String.class);
    verify(toolkit).getSet("_tc_quartz_grp_triggers_scheduledRefresh_" + managerName + "_" + cacheName + "_grp|scheduledRefresh_" + managerName + "_" + cacheName, String.class);

    verify(toolkit).getSortedSet("_tc_time_trigger_sorted_set|scheduledRefresh_" + managerName + "_" + cacheName, TimeTrigger.class);
    
    verify(toolkit, atLeast(1)).getProperties();
    
    verify(toolkit, times(2)).shutdown();
    allowNonPersistentInteractions(toolkit);
    verifyNoMoreInteractions(toolkit);
  }
  
  public static class NullCacheWriterFactory extends CacheWriterFactory {

    @Override
    public CacheWriter createCacheWriter(Ehcache cache, Properties properties) {
      return new AbstractCacheWriter() {
        @Override
        public void write(Element element) throws CacheException {
          //no-op
        }
      };
    }
  }
}

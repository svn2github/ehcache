/*
 * [ * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.terracotta.modules.ehcache.ToolkitClientAccessor;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitInstantiationException;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import java.io.InputStream;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;

public abstract class ClientBase extends AbstractClientBase {

  private final String   name;
  protected CacheManager cacheManager;
  private ToolkitBarrier barrier;
  private Toolkit        toolkit;

  public ClientBase(String[] args) {
    this("test", args);
  }

  public ClientBase(String cacheName, String args[]) {
    super(args);
    this.name = cacheName;
  }

  @Override
  public void doTest() throws Throwable {
    setupCacheManager();
    if (isStandaloneCfg()) {
      runTest(getCache(), null);
    } else {
      runTest(getCache(), getClusteringToolkit());
    }
  }

  protected boolean isStandaloneCfg() {
    return getTestControlMbean().isStandAloneTest();
  }

  protected synchronized final ToolkitBarrier getBarrierForAllClients() {
    if (barrier == null) {
      barrier = getClusteringToolkit().getBarrier("barrier with all clients", getParticipantCount());
    }
    return barrier;
  }

  protected final int waitForAllClients() throws InterruptedException, BrokenBarrierException {
    if (isStandaloneCfg()) return 0;
    return getBarrierForAllClients().await();
  }

  protected void setupCacheManager() {
    cacheManager = CacheManager.create(getEhcacheXmlAsStream());
  }

  protected InputStream getEhcacheXmlAsStream() {
    return Client1.class.getResourceAsStream("/ehcache-config.xml");
  }

  protected Cache getCache() {
    return cacheManager.getCache(name);
  }

  protected CacheManager getCacheManager() {
    return cacheManager;
  }

  protected synchronized Toolkit getClusteringToolkit() {
    if (toolkit == null) {
      toolkit = createToolkit();
    }
    return toolkit;
  }

  protected Toolkit createToolkit() {
    try {
      return ToolkitFactory.createToolkit(getTerracottaTypeSubType() + getTerracottaUrl());
    } catch (ToolkitInstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  protected String getTerracottaTypeSubType() {
    return "toolkit:terracotta://";
  }

  public synchronized void clearTerracottaClient() {
    cacheManager = null;
    barrier = null;
    toolkit = null;
  }

  protected abstract void runTest(Cache cache, Toolkit myToolkit) throws Throwable;

  /**
   * MNK-5309 : Since this method can be called for caches with timeout behavior other than EXCEPTION, we temporarily
   * change the timeout behavior to EXCEPTION during this method and revert it back while exiting.
   */
  public void waitUntilCacheInitialized(Cache cache) throws InterruptedException {
    TimeoutBehaviorConfiguration actualTimeoutBehavior = cache.getCacheConfiguration().getTerracottaConfiguration()
        .getNonstopConfiguration().getTimeoutBehavior();
    try {
      TimeoutBehaviorConfiguration clone = (TimeoutBehaviorConfiguration) actualTimeoutBehavior.clone();
      clone.setType("exception");
      cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration().addTimeoutBehavior(clone);
    } catch (CloneNotSupportedException re) {
      throw new RuntimeException(re);
    }
    
    while (true) {
      try {
        debug("===== Waiting for cache " + cache.getName() + " to be initialized =====");
        cache.put(new Element("key", "value"));
        cache.remove("key");
        cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration()
            .addTimeoutBehavior(actualTimeoutBehavior);
        return;
      } catch (NonStopCacheException e) {
        TimeUnit.SECONDS.sleep(1L);
      }
    }
  }

  public void waitForAllCurrentTransactionsToComplete(Cache cache) {
    // Only do waitFor All Txn for Clustered Caches
    if (cache.getCacheConfiguration().isTerracottaClustered()) {
      Toolkit internalToolkit = ToolkitClientAccessor.getInternalToolkitClient(cache);
      waitForAllCurrentTransactionsToComplete(internalToolkit);
    }
  }

  public void waitForAllCurrentTransactionsToComplete(Toolkit toolkitParam) {
    ((ToolkitInternal) toolkitParam).waitUntilAllTransactionsComplete();
  }

}

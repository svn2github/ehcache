/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.nonstop;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;
import net.sf.ehcache.writer.writebehind.NonStopWriteBehind;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.terracotta.modules.ehcache.ClusteredCacheInternalContext;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.concurrency.NonStopCacheLockProvider;
import org.terracotta.modules.ehcache.store.ToolkitNonStopExceptionOnTimeoutConfiguration;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.rejoin.RejoinException;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class NonStopStoreWrapper implements TerracottaStore {
  private static final long                                   TIME_TO_WAIT_FOR_ASYNC_STORE_INIT = Long
                                                                                                    .parseLong(System
                                                                                                        .getProperty("com.tc.non.stop.async.store.init",
                                                                                                                     String
                                                                                                                         .valueOf(TimeUnit.MINUTES
                                                                                                                             .toMillis(5))));
  private static final Set<String>                            LOCAL_METHODS                     = new HashSet<String>();
  private static final long                                   REJOIN_RETRY_INTERVAL             = 10 * 1000;

  static {
    LOCAL_METHODS.add("unsafeGet");
    LOCAL_METHODS.add("containsKeyInMemory");
    LOCAL_METHODS.add("containsKeyOffHeap");
    LOCAL_METHODS.add("getInMemorySizeInBytes");
    LOCAL_METHODS.add("getInMemorySize");
    LOCAL_METHODS.add("getOffHeapSizeInBytes");
    LOCAL_METHODS.add("getOffHeapSize");
    LOCAL_METHODS.add("getLocalKeys");
  }

  private volatile TerracottaStore                            delegate;
  private final NonStopFeature                                nonStop;
  private final ToolkitNonStopExceptionOnTimeoutConfiguration toolkitNonStopConfiguration;
  private final NonstopConfiguration                          ehcacheNonStopConfiguration;
  private volatile TerracottaStore                            localReadDelegate;
  private final BulkOpsToolkitNonStopConfiguration            bulkOpsToolkitNonStopConfiguration;
  private final ClusteredCacheInternalContext                 clusteredCacheInternalContext;

  private final Ehcache                                       cache;

  private WriteBehind                                         writeBehind;

  public NonStopStoreWrapper(Callable<TerracottaStore> clusteredStoreCreator,
                             ToolkitInstanceFactory toolkitInstanceFactory, Ehcache cache) {
    this.cache = cache;
    this.nonStop = toolkitInstanceFactory.getToolkit().getFeature(ToolkitFeatureType.NONSTOP);
    this.ehcacheNonStopConfiguration = cache.getCacheConfiguration().getTerracottaConfiguration()
        .getNonstopConfiguration();
    this.toolkitNonStopConfiguration = new ToolkitNonStopExceptionOnTimeoutConfiguration(ehcacheNonStopConfiguration);
    this.bulkOpsToolkitNonStopConfiguration = new BulkOpsToolkitNonStopConfiguration(ehcacheNonStopConfiguration);

    Toolkit toolkit = toolkitInstanceFactory.getToolkit();
    CacheLockProvider cacheLockProvider = createCacheLockProvider(toolkit, toolkitInstanceFactory);
    this.clusteredCacheInternalContext = new ClusteredCacheInternalContext(toolkit, cacheLockProvider);
    if (ehcacheNonStopConfiguration != null && ehcacheNonStopConfiguration.isEnabled()) {
      createStoreAsynchronously(toolkit, clusteredStoreCreator);
    } else {
      doInit(clusteredStoreCreator);
    }
  }

  private CacheLockProvider createCacheLockProvider(Toolkit toolkit, ToolkitInstanceFactory toolkitInstanceFactory) {
    return new NonStopCacheLockProvider(toolkit.getFeature(ToolkitFeatureType.NONSTOP), ehcacheNonStopConfiguration,
                                        toolkitInstanceFactory);
  }

  private void createStoreAsynchronously(final Toolkit toolkit, Callable<TerracottaStore> clusteredStoreCreator) {
    Thread t = new Thread(createInitRunnable(clusteredStoreCreator), "init Store asynchronously " + cache.getName());
    t.setDaemon(true);
    t.start();
  }

  private Runnable createInitRunnable(final Callable<TerracottaStore> clusteredStoreCreator) {
    final Runnable initRunnable = new Runnable() {
      @Override
      public void run() {
        long startTime = System.currentTimeMillis();
        while (true) {
          nonStop.start(new ToolkitNonstopDisableConfig());
          try {
            doInit(clusteredStoreCreator);
            synchronized (NonStopStoreWrapper.this) {
              NonStopStoreWrapper.this.notifyAll();
            }
            return;
          } catch (RejoinException e) {
            if (startTime + TIME_TO_WAIT_FOR_ASYNC_STORE_INIT < System.currentTimeMillis()) { throw new RuntimeException(
                                                                                                                         "Unable to create clusteredStore in time",
                                                                                                                         e); }
          } finally {
            nonStop.finish();
          }
        }
      }

    };
    return initRunnable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getInternalContext() {
    return clusteredCacheInternalContext;
  }

  @Override
  public WriteBehind createWriteBehind() {
    if (ehcacheNonStopConfiguration != null && ehcacheNonStopConfiguration.isEnabled()) {
      synchronized (this) {
        if (writeBehind != null) { throw new IllegalStateException(); }

        writeBehind = new NonStopWriteBehind();
        if (delegate != null) {
          ((NonStopWriteBehind) writeBehind).init(cache.getCacheManager().createTerracottaWriteBehind(cache));
        }
        return writeBehind;
      }
    }

    writeBehind = cache.getCacheManager().createTerracottaWriteBehind(cache);
    return writeBehind;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean bufferFull() {
    return false;
  }

  private TerracottaStore createStore(Callable<TerracottaStore> clusteredStoreCreator) {
    try {
      return clusteredStoreCreator.call();
    } catch (InvalidConfigurationException e) {
      throw e;
    } catch (RejoinException e) {
      // can get RejoinException If Rejoin starts during initialization. since NonStop is disabled here.
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void doInit(Callable<TerracottaStore> clusteredStoreCreator) {
    TerracottaStore delegateTemp = createStore(clusteredStoreCreator);

    if (clusteredCacheInternalContext.getCacheLockProvider() instanceof NonStopCacheLockProvider) {
      ((NonStopCacheLockProvider) clusteredCacheInternalContext.getCacheLockProvider())
          .init((CacheLockProvider) delegateTemp.getInternalContext());
    }

    // create this to be sure that it's present on each node to receive clustered events,
    // even if this node is not sending out its events
    cache.getCacheManager().createTerracottaEventReplicator(cache);

    synchronized (this) {
      if (delegate == null) {
        this.delegate = delegateTemp;
        StatisticsManager.associate(this).withChild(delegateTemp);

        if (writeBehind != null && writeBehind instanceof NonStopWriteBehind) {
          ((NonStopWriteBehind) writeBehind).init(cache.getCacheManager().createTerracottaWriteBehind(cache));
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getMBean() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeStoreListener(StoreListener arg0) {
    // TODO: better fix needed ... put here because of CacheManager shutdown
    if (delegate != null) {
      delegate.removeStoreListener(arg0);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    if (delegate != null) {
      this.delegate.dispose();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException,
      InterruptedException {
    nonStop.start(new ToolkitNonstopDisableConfig());
    try {
      waitForInit(Long.MAX_VALUE);
      while (true) {
        try {
          this.delegate.waitUntilClusterCoherent();
          return;
        } catch (RejoinException e) {
          // expected RejoinException since nonstop is disabled.
          Thread.sleep(REJOIN_RETRY_INTERVAL);
        }
      }
    } finally {
      nonStop.finish();
    }
  }

  private void throwNonStopExceptionWhenClusterNotInit() throws NonStopException {
    if (delegate == null && ehcacheNonStopConfiguration != null && ehcacheNonStopConfiguration.isEnabled()) {
      if (ehcacheNonStopConfiguration.isImmediateTimeout()) {
        throw new NonStopException("Cluster not up OR still in the process of connecting ");
      } else {
        long timeout = ehcacheNonStopConfiguration.getTimeoutMillis();
        waitForInit(timeout);
      }
    }
  }

  private void waitForInit(long timeout) {
    synchronized (this) {
      while (delegate == null) {
        try {
          this.wait(timeout);
        } catch (InterruptedException e) {
          // TODO: remove this ... Interrupted here means aborted
          throw new NonStopException("Cluster not up OR still in the process of connecting ");
        }
      }
    }
  }

  private TerracottaStore getTimeoutBehavior() {
    if (ehcacheNonStopConfiguration == null) { throw new AssertionError("Ehcache NonStopConfig cannot be null"); }

    TimeoutBehaviorConfiguration behaviorConfiguration = ehcacheNonStopConfiguration.getTimeoutBehavior();
    switch (behaviorConfiguration.getTimeoutBehaviorType()) {
      case EXCEPTION:
        return ExceptionOnTimeoutStore.getInstance();
      case LOCAL_READS:
        if (localReadDelegate == null) {
          if (delegate == null) { return NoOpOnTimeoutStore.getInstance(); }
          localReadDelegate = new LocalReadsOnTimeoutStore(delegate);
        }
        return localReadDelegate;
      case LOCAL_READS_AND_EXCEPTION_ON_WRITES:
        if (localReadDelegate == null) {
          if (delegate == null) {
            return new LocalReadsAndExceptionOnWritesTimeoutStore();
          } else {
            localReadDelegate = new LocalReadsAndExceptionOnWritesTimeoutStore(delegate);
          }
        }
        return localReadDelegate;
      case NOOP:
        return NoOpOnTimeoutStore.getInstance();
      default:
        return ExceptionOnTimeoutStore.getInstance();
    }
  }

  private static class BulkOpsToolkitNonStopConfiguration extends ToolkitNonStopExceptionOnTimeoutConfiguration {

    public BulkOpsToolkitNonStopConfiguration(NonstopConfiguration ehcacheNonStopConfig) {
      super(ehcacheNonStopConfig);
    }

    @Override
    public long getTimeoutMillis() {
      return ehcacheNonStopConfig.getBulkOpsTimeoutMultiplyFactor() * ehcacheNonStopConfig.getTimeoutMillis();
    }

  }

  private static void validateMethodNamesExist(Class klazz, Set<String> methodToCheck) {
    for (String methodName : methodToCheck) {
      if (!exist(klazz, methodName)) { throw new AssertionError("Method " + methodName + " does not exist in class "
                                                                + klazz.getName()); }
    }
  }

  private static boolean exist(Class klazz, String method) {
    Method[] methods = klazz.getMethods();
    for (Method m : methods) {
      if (m.getName().equals(method)) { return true; }
    }
    return false;
  }

  public static void main(String[] args) {
    PrintStream out = System.out;
    Class[] classes = { TerracottaStore.class };
    Set<String> bulkMethods = new HashSet<String>();
    bulkMethods.add("setNodeCoherent");
    bulkMethods.add("putAll");
    bulkMethods.add("getAllQuiet");
    bulkMethods.add("getAll");
    bulkMethods.add("removeAll");
    validateMethodNamesExist(TerracottaStore.class, bulkMethods);
    for (Class c : classes) {
      for (Method m : c.getMethods()) {
        out.println("/**");
        out.println("* {@inheritDoc}");
        out.println("*/");
        out.print("public " + m.getReturnType().getSimpleName() + " " + m.getName() + "(");
        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
          out.print(params[i].getSimpleName() + " arg" + i);
          if (i < params.length - 1) {
            out.print(", ");
          }
        }
        out.print(")");

        Class<?>[] exceptions = m.getExceptionTypes();

        if (exceptions.length > 0) {
          out.print(" throws ");
        }
        for (int i = 0; i < exceptions.length; i++) {
          out.print(exceptions[i].getSimpleName());
          if (i < exceptions.length - 1) {
            out.print(", ");
          }
        }

        out.println(" {");

        if (LOCAL_METHODS.contains(m.getName())) {
          out.println(" if (delegate != null) {");
          if (m.getReturnType() != Void.TYPE) {
            out.print("return ");
          }

          if (NonStopSubTypeProxyUtil.isNonStopSubtype(m.getReturnType())) {
            out.print("NonStopSubTypeProxyUtil.newNonStopSubTypeProxy(" + m.getReturnType().getSimpleName()
                      + ".class , ");
          }
          out.print("this.delegate." + m.getName() + "(");
          for (int i = 0; i < params.length; i++) {
            out.print("arg" + i);
            if (i < params.length - 1) {
              out.print(", ");
            }
          }
          if (NonStopSubTypeProxyUtil.isNonStopSubtype(m.getReturnType())) {
            out.println(")");
          }
          out.println(");");

          out.println("    } else {");

          if (m.getReturnType() != Void.TYPE) {
            out.print("return ");
          }

          out.print("NoOpOnTimeoutStore.getInstance()." + m.getName() + "(");
          for (int i = 0; i < params.length; i++) {
            out.print("arg" + i);
            if (i < params.length - 1) {
              out.print(", ");
            }
          }
          out.println(");");

          out.println(" }");
          out.println(" }");
        } else {

          out.println("    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!");
          if (bulkMethods.contains(m.getName())) {
            out.println("      nonStop.start(bulkOpsToolkitNonStopConfiguration);");
          } else {
            out.println("      nonStop.start(toolkitNonStopConfiguration);");
          }
          out.println("      try {");

          out.println("      throwNonStopExceptionWhenClusterNotInit();");

          out.print("        ");
          if (m.getReturnType() != Void.TYPE) {
            out.print("return ");
          }
          if (NonStopSubTypeProxyUtil.isNonStopSubtype(m.getReturnType())) {
            out.print("NonStopSubTypeProxyUtil.newNonStopSubTypeProxy(" + m.getReturnType().getSimpleName()
                      + ".class , ");
          }
          out.print("this.delegate." + m.getName() + "(");
          for (int i = 0; i < params.length; i++) {
            out.print("arg" + i);
            if (i < params.length - 1) {
              out.print(", ");
            }
          }
          if (NonStopSubTypeProxyUtil.isNonStopSubtype(m.getReturnType())) {
            out.println(")");
          }
          out.println(");");
          out.println("      } catch (NonStopException e) {");
          if (m.getReturnType() != Void.TYPE) {
            out.print("return ");
          }
          out.print("getTimeoutBehavior()." + m.getName() + "(");
          for (int i = 0; i < params.length; i++) {
            out.print("arg" + i);
            if (i < params.length - 1) {
              out.print(", ");
            }
          }
          out.println(");");

          out.println("      } catch (RejoinException e) {");
          if (m.getReturnType() != Void.TYPE) {
            out.print("return ");
          }
          out.print("getTimeoutBehavior()." + m.getName() + "(");
          for (int i = 0; i < params.length; i++) {
            out.print("arg" + i);
            if (i < params.length - 1) {
              out.print(", ");
            }
          }
          out.println(");");
          out.println("      } finally {");
          out.println("        nonStop.finish();");
          out.println("      }");
          out.println("}");
          out.println("");
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element unsafeGet(Object arg0) {
    if (delegate != null) {
      return this.delegate.unsafeGet(arg0);
    } else {
      return NoOpOnTimeoutStore.getInstance().unsafeGet(arg0);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set getLocalKeys() {
    if (delegate != null) {
      return NonStopSubTypeProxyUtil.newNonStopSubTypeProxy(Set.class, this.delegate.getLocalKeys());
    } else {
      return NoOpOnTimeoutStore.getInstance().getLocalKeys();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransactionalMode getTransactionalMode() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.getTransactionalMode();
    } catch (NonStopException e) {
      return getTimeoutBehavior().getTransactionalMode();
    } catch (RejoinException e) {
      return getTimeoutBehavior().getTransactionalMode();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element get(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.get(arg0);
    } catch (NonStopException e) {
      return getTimeoutBehavior().get(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().get(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean put(Element arg0) throws CacheException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.put(arg0);
    } catch (NonStopException e) {
      return getTimeoutBehavior().put(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().put(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean replace(Element arg0, Element arg1, ElementValueComparator arg2) throws NullPointerException,
      IllegalArgumentException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.replace(arg0, arg1, arg2);
    } catch (NonStopException e) {
      return getTimeoutBehavior().replace(arg0, arg1, arg2);
    } catch (RejoinException e) {
      return getTimeoutBehavior().replace(arg0, arg1, arg2);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element replace(Element arg0) throws NullPointerException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.replace(arg0);
    } catch (NonStopException e) {
      return getTimeoutBehavior().replace(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().replace(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putAll(Collection arg0) throws CacheException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.putAll(arg0);
    } catch (NonStopException e) {
      getTimeoutBehavior().putAll(arg0);
    } catch (RejoinException e) {
      getTimeoutBehavior().putAll(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element remove(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.remove(arg0);
    } catch (NonStopException e) {
      return getTimeoutBehavior().remove(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().remove(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() throws IOException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.flush();
    } catch (NonStopException e) {
      getTimeoutBehavior().flush();
    } catch (RejoinException e) {
      getTimeoutBehavior().flush();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKey(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.containsKey(arg0);
    } catch (NonStopException e) {
      return getTimeoutBehavior().containsKey(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().containsKey(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getSize() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.getSize();
    } catch (NonStopException e) {
      return getTimeoutBehavior().getSize();
    } catch (RejoinException e) {
      return getTimeoutBehavior().getSize();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAll() throws CacheException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.removeAll();
    } catch (NonStopException e) {
      getTimeoutBehavior().removeAll();
    } catch (RejoinException e) {
      getTimeoutBehavior().removeAll();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAll(Collection arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.removeAll(arg0);
    } catch (NonStopException e) {
      getTimeoutBehavior().removeAll(arg0);
    } catch (RejoinException e) {
      getTimeoutBehavior().removeAll(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element removeElement(Element arg0, ElementValueComparator arg1) throws NullPointerException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.removeElement(arg0, arg1);
    } catch (NonStopException e) {
      return getTimeoutBehavior().removeElement(arg0, arg1);
    } catch (RejoinException e) {
      return getTimeoutBehavior().removeElement(arg0, arg1);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element putIfAbsent(Element arg0) throws NullPointerException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.putIfAbsent(arg0);
    } catch (NonStopException e) {
      return getTimeoutBehavior().putIfAbsent(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().putIfAbsent(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyInMemory(Object arg0) {
    if (delegate != null) {
      return this.delegate.containsKeyInMemory(arg0);
    } else {
      return NoOpOnTimeoutStore.getInstance().containsKeyInMemory(arg0);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyOffHeap(Object arg0) {
    if (delegate != null) {
      return this.delegate.containsKeyOffHeap(arg0);
    } else {
      return NoOpOnTimeoutStore.getInstance().containsKeyOffHeap(arg0);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getInMemorySizeInBytes() {
    if (delegate != null) {
      return this.delegate.getInMemorySizeInBytes();
    } else {
      return NoOpOnTimeoutStore.getInstance().getInMemorySizeInBytes();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getInMemorySize() {
    if (delegate != null) {
      return this.delegate.getInMemorySize();
    } else {
      return NoOpOnTimeoutStore.getInstance().getInMemorySize();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getOffHeapSizeInBytes() {
    if (delegate != null) {
      return this.delegate.getOffHeapSizeInBytes();
    } else {
      return NoOpOnTimeoutStore.getInstance().getOffHeapSizeInBytes();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOffHeapSize() {
    if (delegate != null) {
      return this.delegate.getOffHeapSize();
    } else {
      return NoOpOnTimeoutStore.getInstance().getOffHeapSize();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setNodeCoherent(boolean arg0) throws UnsupportedOperationException, TerracottaNotRunningException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.setNodeCoherent(arg0);
    } catch (NonStopException e) {
      getTimeoutBehavior().setNodeCoherent(arg0);
    } catch (RejoinException e) {
      getTimeoutBehavior().setNodeCoherent(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map getAllQuiet(Collection arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return NonStopSubTypeProxyUtil.newNonStopSubTypeProxy(Map.class, this.delegate.getAllQuiet(arg0));
    } catch (NonStopException e) {
      return getTimeoutBehavior().getAllQuiet(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().getAllQuiet(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map getAll(Collection arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return NonStopSubTypeProxyUtil.newNonStopSubTypeProxy(Map.class, this.delegate.getAll(arg0));
    } catch (NonStopException e) {
      return getTimeoutBehavior().getAll(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().getAll(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAttributeExtractors(Map arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.setAttributeExtractors(arg0);
    } catch (NonStopException e) {
      getTimeoutBehavior().setAttributeExtractors(arg0);
    } catch (RejoinException e) {
      getTimeoutBehavior().setAttributeExtractors(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasAbortedSizeOf() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.hasAbortedSizeOf();
    } catch (NonStopException e) {
      return getTimeoutBehavior().hasAbortedSizeOf();
    } catch (RejoinException e) {
      return getTimeoutBehavior().hasAbortedSizeOf();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOnDiskSize() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.getOnDiskSize();
    } catch (NonStopException e) {
      return getTimeoutBehavior().getOnDiskSize();
    } catch (RejoinException e) {
      return getTimeoutBehavior().getOnDiskSize();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setInMemoryEvictionPolicy(Policy arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.setInMemoryEvictionPolicy(arg0);
    } catch (NonStopException e) {
      getTimeoutBehavior().setInMemoryEvictionPolicy(arg0);
    } catch (RejoinException e) {
      getTimeoutBehavior().setInMemoryEvictionPolicy(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Results executeQuery(StoreQuery arg0) throws SearchException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.executeQuery(arg0);
    } catch (NonStopException e) {
      return getTimeoutBehavior().executeQuery(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().executeQuery(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putWithWriter(Element arg0, CacheWriterManager arg1) throws CacheException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.putWithWriter(arg0, arg1);
    } catch (NonStopException e) {
      return getTimeoutBehavior().putWithWriter(arg0, arg1);
    } catch (RejoinException e) {
      return getTimeoutBehavior().putWithWriter(arg0, arg1);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recalculateSize(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.recalculateSize(arg0);
    } catch (NonStopException e) {
      getTimeoutBehavior().recalculateSize(arg0);
    } catch (RejoinException e) {
      getTimeoutBehavior().recalculateSize(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element getQuiet(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.getQuiet(arg0);
    } catch (NonStopException e) {
      return getTimeoutBehavior().getQuiet(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().getQuiet(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCacheCoherent() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.isCacheCoherent();
    } catch (NonStopException e) {
      return getTimeoutBehavior().isCacheCoherent();
    } catch (RejoinException e) {
      return getTimeoutBehavior().isCacheCoherent();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getOnDiskSizeInBytes() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.getOnDiskSizeInBytes();
    } catch (NonStopException e) {
      return getTimeoutBehavior().getOnDiskSizeInBytes();
    } catch (RejoinException e) {
      return getTimeoutBehavior().getOnDiskSizeInBytes();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getTerracottaClusteredSize() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.getTerracottaClusteredSize();
    } catch (NonStopException e) {
      return getTimeoutBehavior().getTerracottaClusteredSize();
    } catch (RejoinException e) {
      return getTimeoutBehavior().getTerracottaClusteredSize();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void expireElements() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.expireElements();
    } catch (NonStopException e) {
      getTimeoutBehavior().expireElements();
    } catch (RejoinException e) {
      getTimeoutBehavior().expireElements();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isNodeCoherent() throws TerracottaNotRunningException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.isNodeCoherent();
    } catch (NonStopException e) {
      return getTimeoutBehavior().isNodeCoherent();
    } catch (RejoinException e) {
      return getTimeoutBehavior().isNodeCoherent();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addStoreListener(StoreListener arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.addStoreListener(arg0);
    } catch (NonStopException e) {
      getTimeoutBehavior().addStoreListener(arg0);
    } catch (RejoinException e) {
      getTimeoutBehavior().addStoreListener(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isClusterCoherent() throws TerracottaNotRunningException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.isClusterCoherent();
    } catch (NonStopException e) {
      return getTimeoutBehavior().isClusterCoherent();
    } catch (RejoinException e) {
      return getTimeoutBehavior().isClusterCoherent();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Policy getInMemoryEvictionPolicy() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.getInMemoryEvictionPolicy();
    } catch (NonStopException e) {
      return getTimeoutBehavior().getInMemoryEvictionPolicy();
    } catch (RejoinException e) {
      return getTimeoutBehavior().getInMemoryEvictionPolicy();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element removeWithWriter(Object arg0, CacheWriterManager arg1) throws CacheException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.removeWithWriter(arg0, arg1);
    } catch (NonStopException e) {
      return getTimeoutBehavior().removeWithWriter(arg0, arg1);
    } catch (RejoinException e) {
      return getTimeoutBehavior().removeWithWriter(arg0, arg1);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List getKeys() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return NonStopSubTypeProxyUtil.newNonStopSubTypeProxy(List.class, this.delegate.getKeys());
    } catch (NonStopException e) {
      return getTimeoutBehavior().getKeys();
    } catch (RejoinException e) {
      return getTimeoutBehavior().getKeys();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Status getStatus() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.getStatus();
    } catch (NonStopException e) {
      return getTimeoutBehavior().getStatus();
    } catch (RejoinException e) {
      return getTimeoutBehavior().getStatus();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Attribute getSearchAttribute(String arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.getSearchAttribute(arg0);
    } catch (NonStopException e) {
      return getTimeoutBehavior().getSearchAttribute(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().getSearchAttribute(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyOnDisk(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      return this.delegate.containsKeyOnDisk(arg0);
    } catch (NonStopException e) {
      return getTimeoutBehavior().containsKeyOnDisk(arg0);
    } catch (RejoinException e) {
      return getTimeoutBehavior().containsKeyOnDisk(arg0);
    } finally {
      nonStop.finish();
    }
  }

}

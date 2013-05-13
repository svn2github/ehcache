/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.nonstop;

import static net.sf.ehcache.statistics.StatisticBuilder.operation;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.NonStopOperationOutcomes;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.event.CacheEventListener;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.ClusteredCacheInternalContext;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.concurrency.NonStopCacheLockProvider;
import org.terracotta.modules.ehcache.store.TerracottaStoreInitializationService;
import org.terracotta.modules.ehcache.store.ToolkitNonStopExceptionOnTimeoutConfiguration;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.observer.OperationObserver;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.nonstop.NonStopToolkitInstantiationException;
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

  private static final Logger                                                      LOGGER                            = LoggerFactory
                                                                                                                         .getLogger(NonStopStoreWrapper.class);

  private static final long                                                        TIME_TO_WAIT_FOR_ASYNC_STORE_INIT = Long
                                                                                                                         .getLong("com.tc.non.stop.async.store.init",
                                                                                                                                  TimeUnit.MINUTES
                                                                                                                                      .toMillis(5));

  private static final Set<String>                            LOCAL_METHODS                     = new HashSet<String>();
  private static final long                                                        REJOIN_RETRY_INTERVAL             = 10 * 1000;

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

  private static final Set<String>                                                 METHODS_TO_SKIP                   = new HashSet<String>();

  static {
    METHODS_TO_SKIP
        .add("public abstract net.sf.ehcache.writer.writebehind.WriteBehind net.sf.ehcache.store.TerracottaStore.createWriteBehind()");
    METHODS_TO_SKIP.add("public abstract java.lang.Object net.sf.ehcache.store.Store.getInternalContext()");
    METHODS_TO_SKIP.add("public abstract boolean net.sf.ehcache.store.Store.bufferFull()");
    METHODS_TO_SKIP.add("public abstract java.lang.Object net.sf.ehcache.store.Store.getMBean()");
    METHODS_TO_SKIP.add("public abstract void net.sf.ehcache.store.Store.dispose()");
    METHODS_TO_SKIP
        .add("public abstract void net.sf.ehcache.store.Store.removeStoreListener(net.sf.ehcache.store.StoreListener)");
    METHODS_TO_SKIP
        .add("public abstract void net.sf.ehcache.store.Store.waitUntilClusterCoherent() throws java.lang.UnsupportedOperationException,net.sf.ehcache.terracotta.TerracottaNotRunningException,java.lang.InterruptedException");
  }

  private volatile TerracottaStore                                                 delegate;
  private final NonStopFeature                                                     nonStop;
  private final ToolkitNonStopExceptionOnTimeoutConfiguration                      toolkitNonStopConfiguration;
  private final NonstopConfiguration                                               ehcacheNonStopConfiguration;
  private volatile TerracottaStore                                                 localReadDelegate;
  private final BulkOpsToolkitNonStopConfiguration                                 bulkOpsToolkitNonStopConfiguration;
  private final ClusteredCacheInternalContext                                      clusteredCacheInternalContext;
  private final TerracottaStoreInitializationService                               initializationService;

  private final Ehcache                                                            cache;

  private WriteBehind                                                              writeBehind;
  private volatile Throwable                                                       exceptionDuringInitialization     = null;
  private final OperationObserver<CacheOperationOutcomes.NonStopOperationOutcomes> nonstopObserver                   = operation(
                                                                                                                                 NonStopOperationOutcomes.class)
                                                                                                                         .named("nonstop")
                                                                                                                         .of(this)
                                                                                                                         .tag("cache")
                                                                                                                         .build();

  private CacheEventListener                                                       cacheEventListener;

  public NonStopStoreWrapper(Callable<TerracottaStore> clusteredStoreCreator,
                             ToolkitInstanceFactory toolkitInstanceFactory, Ehcache cache,
                             TerracottaStoreInitializationService initializationService) {
    this.cache = cache;
    this.initializationService = initializationService;
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
      createStore(clusteredStoreCreator);
    }
    StatisticsManager.associate(this).withParent(cache);
  }

  private void createStore(Callable<TerracottaStore> clusteredStoreCreator) {
    try {
      while (true) {
        try {
          doInit(clusteredStoreCreator);
          return;
        } catch (RejoinException e) {
          // ignore RejoinException and wait for REJOIN_RETRY_INTERVAL before retry
          Thread.sleep(REJOIN_RETRY_INTERVAL);
        }
      }
    } catch (Throwable t) {
      String message = "Error while creating store inline ";
      handleException(message, t);
    }
  }

  private CacheLockProvider createCacheLockProvider(Toolkit toolkit, ToolkitInstanceFactory toolkitInstanceFactory) {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    return new NonStopCacheLockProvider(toolkit.getFeature(ToolkitFeatureType.NONSTOP), ehcacheNonStopConfiguration,
                                        toolkitInstanceFactory);
  }

  private void createStoreAsynchronously(final Toolkit toolkit, Callable<TerracottaStore> clusteredStoreCreator) {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    initializationService.initialize(createInitRunnable(clusteredStoreCreator), ehcacheNonStopConfiguration);
    if (exceptionDuringInitialization != null) { throw new NonStopToolkitInstantiationException(
                                                                                                exceptionDuringInitialization); }
  }

  private Runnable createInitRunnable(final Callable<TerracottaStore> clusteredStoreCreator) {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    final Runnable initRunnable = new Runnable() {
      @Override
      public void run() {
        try {
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
        } catch (Throwable t) {
          LOGGER.warn("Error while creating store asynchronously for Cache: " + cache.getName(), t);
          exceptionDuringInitialization = t;
          synchronized (NonStopStoreWrapper.this) {
            NonStopStoreWrapper.this.notifyAll();
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
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    return clusteredCacheInternalContext;
  }

  @Override
  public WriteBehind createWriteBehind() {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
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
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    return false;
  }

  private TerracottaStore createTerracottaStore(Callable<TerracottaStore> clusteredStoreCreator) {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
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
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    TerracottaStore delegateTemp = createTerracottaStore(clusteredStoreCreator);

    if (clusteredCacheInternalContext.getCacheLockProvider() instanceof NonStopCacheLockProvider) {
      ((NonStopCacheLockProvider) clusteredCacheInternalContext.getCacheLockProvider())
          .init((CacheLockProvider) delegateTemp.getInternalContext());
    }

    // create this to be sure that it's present on each node to receive clustered events,
    // even if this node is not sending out its events
    cacheEventListener = cache.getCacheManager().createTerracottaEventReplicator(cache);

    synchronized (this) {
      if (delegate == null) {
        this.delegate = delegateTemp;
        StatisticsManager.associate(this).withChild(delegateTemp);

        if (writeBehind != null && writeBehind instanceof NonStopWriteBehind) {
          ((NonStopWriteBehind) writeBehind).init(cache.getCacheManager().createTerracottaWriteBehind(cache));
        }
      }
    }
    LOGGER.debug("Initialization Completed for Cache : {}", cache.getName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getMBean() {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeStoreListener(StoreListener arg0) {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
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
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    cacheEventListener.dispose();
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
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
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
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
    } finally {
      nonStop.finish();
    }
  }

  private void throwNonStopExceptionWhenClusterNotInit() throws NonStopException {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    if (delegate == null && ehcacheNonStopConfiguration != null && ehcacheNonStopConfiguration.isEnabled()) {
      if (ehcacheNonStopConfiguration.isImmediateTimeout()) {
        if (exceptionDuringInitialization != null) { throw new NonStopToolkitInstantiationException(
                                                                                                    exceptionDuringInitialization); }
        throw new NonStopException("Cluster not up OR still in the process of connecting ");
      } else {
        long timeout = ehcacheNonStopConfiguration.getTimeoutMillis();
        waitForInit(timeout);
      }
    }
  }

  private void handleException(String message, Throwable t) {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    if (t.getClass().getSimpleName().equals("TCNotRunningException")) { throw new TerracottaNotRunningException(
                                                                                                                "Clustered Cache is probably shutdown or Terracotta backend is down.",
                                                                                                                t); }
    if (t instanceof CacheException) {
      throw (CacheException) t;
    }
    throw new CacheException(message + t.getMessage(), t);
  }

  private void handleNonStopToolkitInstantiationException(NonStopToolkitInstantiationException e) {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    switch (ehcacheNonStopConfiguration.getTimeoutBehavior().getTimeoutBehaviorType()) {
      case EXCEPTION:
        nonstopObserver.end(NonStopOperationOutcomes.FAILURE);
        throw new NonStopCacheException("Error while initializing cache", e);
      default:
        LOGGER.error("Error while initializing cache", e);
    }
  }

  private void waitForInit(long timeout) {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    synchronized (this) {
      while (delegate == null) {
        try {
          if (exceptionDuringInitialization != null) { throw new NonStopToolkitInstantiationException(
                                                                                                      exceptionDuringInitialization); }
          this.wait(timeout);
        } catch (InterruptedException e) {
          // TODO: remove this ... Interrupted here means aborted
          throw new NonStopException("Cluster not up OR still in the process of connecting ");
        }
      }
    }
  }

  private TerracottaStore getTimeoutBehavior() {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
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
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    for (String methodName : methodToCheck) {
      if (!exist(klazz, methodName)) { throw new AssertionError("Method " + methodName + " does not exist in class "
                                                                + klazz.getName()); }
    }
  }

  private static boolean exist(Class klazz, String method) {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    Method[] methods = klazz.getMethods();
    for (Method m : methods) {
      if (m.getName().equals(method)) { return true; }
    }
    return false;
  }

  public static void main(String[] args) {
    // THIS IS HAND MADE CODE -- DO NOT GENERATED
    PrintStream out = System.out;
    Class[] classes = { TerracottaStore.class };
    Set<String> bulkMethods = new HashSet<String>();
    bulkMethods.add("setNodeCoherent");
    bulkMethods.add("putAll");
    bulkMethods.add("getAllQuiet");
    bulkMethods.add("getAll");
    bulkMethods.add("removeAll");
    bulkMethods.add("getSize");
    bulkMethods.add("getTerracottaClusteredSize");
    validateMethodNamesExist(TerracottaStore.class, bulkMethods);
    for (Class c : classes) {
      for (Method m : c.getMethods()) {
        if (METHODS_TO_SKIP.contains(m.toGenericString())) {
          continue;
        }
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
        out.println(" // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!");
        out.println(" // " + m.toGenericString());
        if (LOCAL_METHODS.contains(m.getName())) {
          out.println(" if (delegate != null) {");
          if (m.getReturnType() != Void.TYPE) {
            out.print(m.getReturnType().getSimpleName() + " _ret = ");
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
          if (m.getReturnType() != Void.TYPE) {
            out.println("return _ret;");
          }

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

          if (bulkMethods.contains(m.getName())) {
            out.println("      nonStop.start(bulkOpsToolkitNonStopConfiguration);");
          } else {
            out.println("      nonStop.start(toolkitNonStopConfiguration);");
          }
          out.println("      try {");

          out.println("      throwNonStopExceptionWhenClusterNotInit();");

          out.print("        ");

          if (m.getReturnType() != Void.TYPE) {
            out.print(m.getReturnType().getSimpleName() + " _ret = ");
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
          out.println("nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);");

          if (m.getReturnType() != Void.TYPE) {
            out.println("return _ret;");
          }

          out.println("      } catch (NonStopToolkitInstantiationException e) {");
          System.out.println("handleNonStopToolkitInstantiationException(e);");
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
          out.println("      } catch (NonStopException e) {");
          out.println("nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);");
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
          out.println("nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);");
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
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    // public abstract net.sf.ehcache.Element net.sf.ehcache.store.TerracottaStore.unsafeGet(java.lang.Object)
    if (delegate != null) {
      Element _ret = this.delegate.unsafeGet(arg0);
      return _ret;
    } else {
      return NoOpOnTimeoutStore.getInstance().unsafeGet(arg0);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set getLocalKeys() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    // public abstract java.util.Set net.sf.ehcache.store.TerracottaStore.getLocalKeys()
    if (delegate != null) {
      Set _ret = NonStopSubTypeProxyUtil.newNonStopSubTypeProxy(Set.class, this.delegate.getLocalKeys());
      return _ret;
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
    // public abstract net.sf.ehcache.config.CacheConfiguration$TransactionalMode
    // net.sf.ehcache.store.TerracottaStore.getTransactionalMode()
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      TransactionalMode _ret = this.delegate.getTransactionalMode();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getTransactionalMode();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getTransactionalMode();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().getTransactionalMode();
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
    // public abstract net.sf.ehcache.Element net.sf.ehcache.store.Store.remove(java.lang.Object)
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Element _ret = this.delegate.remove(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().remove(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().remove(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().remove(arg0);
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
    // public abstract net.sf.ehcache.Element net.sf.ehcache.store.Store.get(java.lang.Object)
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Element _ret = this.delegate.get(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().get(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().get(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract boolean net.sf.ehcache.store.Store.put(net.sf.ehcache.Element) throws
    // net.sf.ehcache.CacheException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      boolean _ret = this.delegate.put(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().put(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().put(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().put(arg0);
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
    // public abstract void net.sf.ehcache.store.Store.putAll(java.util.Collection<net.sf.ehcache.Element>) throws
    // net.sf.ehcache.CacheException
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.putAll(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      getTimeoutBehavior().putAll(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      getTimeoutBehavior().putAll(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      getTimeoutBehavior().putAll(arg0);
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
    // public abstract net.sf.ehcache.Element net.sf.ehcache.store.Store.putIfAbsent(net.sf.ehcache.Element) throws
    // java.lang.NullPointerException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Element _ret = this.delegate.putIfAbsent(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().putIfAbsent(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().putIfAbsent(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().putIfAbsent(arg0);
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
    // public abstract void net.sf.ehcache.store.Store.flush() throws java.io.IOException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.flush();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      getTimeoutBehavior().flush();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      getTimeoutBehavior().flush();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract boolean net.sf.ehcache.store.Store.containsKey(java.lang.Object)
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      boolean _ret = this.delegate.containsKey(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().containsKey(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().containsKey(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().containsKey(arg0);
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
    // public abstract boolean
    // net.sf.ehcache.store.Store.replace(net.sf.ehcache.Element,net.sf.ehcache.Element,net.sf.ehcache.store.ElementValueComparator)
    // throws java.lang.NullPointerException,java.lang.IllegalArgumentException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      boolean _ret = this.delegate.replace(arg0, arg1, arg2);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().replace(arg0, arg1, arg2);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().replace(arg0, arg1, arg2);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract net.sf.ehcache.Element net.sf.ehcache.store.Store.replace(net.sf.ehcache.Element) throws
    // java.lang.NullPointerException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Element _ret = this.delegate.replace(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().replace(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().replace(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().replace(arg0);
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
    // public abstract int net.sf.ehcache.store.Store.getSize()
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      int _ret = this.delegate.getSize();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getSize();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getSize();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract void net.sf.ehcache.store.Store.removeAll() throws net.sf.ehcache.CacheException
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.removeAll();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      getTimeoutBehavior().removeAll();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      getTimeoutBehavior().removeAll();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract void net.sf.ehcache.store.Store.removeAll(java.util.Collection<?>)
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.removeAll(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      getTimeoutBehavior().removeAll(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      getTimeoutBehavior().removeAll(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract net.sf.ehcache.Element
    // net.sf.ehcache.store.Store.removeElement(net.sf.ehcache.Element,net.sf.ehcache.store.ElementValueComparator)
    // throws java.lang.NullPointerException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Element _ret = this.delegate.removeElement(arg0, arg1);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().removeElement(arg0, arg1);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().removeElement(arg0, arg1);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().removeElement(arg0, arg1);
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
    // public abstract java.util.List net.sf.ehcache.store.Store.getKeys()
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      List _ret = NonStopSubTypeProxyUtil.newNonStopSubTypeProxy(List.class, this.delegate.getKeys());
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getKeys();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getKeys();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().getKeys();
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyInMemory(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    // public abstract boolean net.sf.ehcache.store.Store.containsKeyInMemory(java.lang.Object)
    if (delegate != null) {
      boolean _ret = this.delegate.containsKeyInMemory(arg0);
      return _ret;
    } else {
      return NoOpOnTimeoutStore.getInstance().containsKeyInMemory(arg0);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyOffHeap(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    // public abstract boolean net.sf.ehcache.store.Store.containsKeyOffHeap(java.lang.Object)
    if (delegate != null) {
      boolean _ret = this.delegate.containsKeyOffHeap(arg0);
      return _ret;
    } else {
      return NoOpOnTimeoutStore.getInstance().containsKeyOffHeap(arg0);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getInMemorySizeInBytes() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    // public abstract long net.sf.ehcache.store.Store.getInMemorySizeInBytes()
    if (delegate != null) {
      long _ret = this.delegate.getInMemorySizeInBytes();
      return _ret;
    } else {
      return NoOpOnTimeoutStore.getInstance().getInMemorySizeInBytes();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getInMemorySize() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    // public abstract int net.sf.ehcache.store.Store.getInMemorySize()
    if (delegate != null) {
      int _ret = this.delegate.getInMemorySize();
      return _ret;
    } else {
      return NoOpOnTimeoutStore.getInstance().getInMemorySize();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getOffHeapSizeInBytes() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    // public abstract long net.sf.ehcache.store.Store.getOffHeapSizeInBytes()
    if (delegate != null) {
      long _ret = this.delegate.getOffHeapSizeInBytes();
      return _ret;
    } else {
      return NoOpOnTimeoutStore.getInstance().getOffHeapSizeInBytes();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOffHeapSize() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    // public abstract int net.sf.ehcache.store.Store.getOffHeapSize()
    if (delegate != null) {
      int _ret = this.delegate.getOffHeapSize();
      return _ret;
    } else {
      return NoOpOnTimeoutStore.getInstance().getOffHeapSize();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map getAllQuiet(Collection arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    // public abstract java.util.Map<java.lang.Object, net.sf.ehcache.Element>
    // net.sf.ehcache.store.Store.getAllQuiet(java.util.Collection<?>)
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Map _ret = NonStopSubTypeProxyUtil.newNonStopSubTypeProxy(Map.class, this.delegate.getAllQuiet(arg0));
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getAllQuiet(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getAllQuiet(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract java.util.Map<java.lang.Object, net.sf.ehcache.Element>
    // net.sf.ehcache.store.Store.getAll(java.util.Collection<?>)
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Map _ret = NonStopSubTypeProxyUtil.newNonStopSubTypeProxy(Map.class, this.delegate.getAll(arg0));
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getAll(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getAll(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().getAll(arg0);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setNodeCoherent(boolean arg0) throws UnsupportedOperationException, TerracottaNotRunningException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    // public abstract void net.sf.ehcache.store.Store.setNodeCoherent(boolean) throws
    // java.lang.UnsupportedOperationException,net.sf.ehcache.terracotta.TerracottaNotRunningException
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.setNodeCoherent(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      getTimeoutBehavior().setNodeCoherent(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      getTimeoutBehavior().setNodeCoherent(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      getTimeoutBehavior().setNodeCoherent(arg0);
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
    // public abstract void net.sf.ehcache.store.Store.setAttributeExtractors(java.util.Map<java.lang.String,
    // net.sf.ehcache.search.attribute.AttributeExtractor>)
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.setAttributeExtractors(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      getTimeoutBehavior().setAttributeExtractors(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      getTimeoutBehavior().setAttributeExtractors(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract boolean net.sf.ehcache.store.Store.hasAbortedSizeOf()
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      boolean _ret = this.delegate.hasAbortedSizeOf();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().hasAbortedSizeOf();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().hasAbortedSizeOf();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract int net.sf.ehcache.store.Store.getOnDiskSize()
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      int _ret = this.delegate.getOnDiskSize();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getOnDiskSize();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getOnDiskSize();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract void net.sf.ehcache.store.Store.setInMemoryEvictionPolicy(net.sf.ehcache.store.Policy)
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.setInMemoryEvictionPolicy(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      getTimeoutBehavior().setInMemoryEvictionPolicy(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      getTimeoutBehavior().setInMemoryEvictionPolicy(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      getTimeoutBehavior().setInMemoryEvictionPolicy(arg0);
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
    // public abstract boolean
    // net.sf.ehcache.store.Store.putWithWriter(net.sf.ehcache.Element,net.sf.ehcache.writer.CacheWriterManager) throws
    // net.sf.ehcache.CacheException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      boolean _ret = this.delegate.putWithWriter(arg0, arg1);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().putWithWriter(arg0, arg1);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().putWithWriter(arg0, arg1);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract void net.sf.ehcache.store.Store.recalculateSize(java.lang.Object)
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.recalculateSize(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      getTimeoutBehavior().recalculateSize(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      getTimeoutBehavior().recalculateSize(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      getTimeoutBehavior().recalculateSize(arg0);
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
    // public abstract boolean net.sf.ehcache.store.Store.isCacheCoherent()
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      boolean _ret = this.delegate.isCacheCoherent();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().isCacheCoherent();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().isCacheCoherent();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract long net.sf.ehcache.store.Store.getOnDiskSizeInBytes()
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      long _ret = this.delegate.getOnDiskSizeInBytes();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getOnDiskSizeInBytes();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getOnDiskSizeInBytes();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract int net.sf.ehcache.store.Store.getTerracottaClusteredSize()
    nonStop.start(bulkOpsToolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      int _ret = this.delegate.getTerracottaClusteredSize();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getTerracottaClusteredSize();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getTerracottaClusteredSize();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract void net.sf.ehcache.store.Store.expireElements()
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.expireElements();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      getTimeoutBehavior().expireElements();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      getTimeoutBehavior().expireElements();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract boolean net.sf.ehcache.store.Store.isNodeCoherent() throws
    // net.sf.ehcache.terracotta.TerracottaNotRunningException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      boolean _ret = this.delegate.isNodeCoherent();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().isNodeCoherent();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().isNodeCoherent();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract void net.sf.ehcache.store.Store.addStoreListener(net.sf.ehcache.store.StoreListener)
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      this.delegate.addStoreListener(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      getTimeoutBehavior().addStoreListener(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      getTimeoutBehavior().addStoreListener(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract boolean net.sf.ehcache.store.Store.isClusterCoherent() throws
    // net.sf.ehcache.terracotta.TerracottaNotRunningException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      boolean _ret = this.delegate.isClusterCoherent();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().isClusterCoherent();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().isClusterCoherent();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract net.sf.ehcache.store.Policy net.sf.ehcache.store.Store.getInMemoryEvictionPolicy()
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Policy _ret = this.delegate.getInMemoryEvictionPolicy();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getInMemoryEvictionPolicy();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getInMemoryEvictionPolicy();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract net.sf.ehcache.Element
    // net.sf.ehcache.store.Store.removeWithWriter(java.lang.Object,net.sf.ehcache.writer.CacheWriterManager) throws
    // net.sf.ehcache.CacheException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Element _ret = this.delegate.removeWithWriter(arg0, arg1);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().removeWithWriter(arg0, arg1);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().removeWithWriter(arg0, arg1);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().removeWithWriter(arg0, arg1);
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
    // public abstract <T> net.sf.ehcache.search.Attribute<T>
    // net.sf.ehcache.store.Store.getSearchAttribute(java.lang.String)
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Attribute _ret = this.delegate.getSearchAttribute(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getSearchAttribute(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getSearchAttribute(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
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
    // public abstract boolean net.sf.ehcache.store.Store.containsKeyOnDisk(java.lang.Object)
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      boolean _ret = this.delegate.containsKeyOnDisk(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().containsKeyOnDisk(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().containsKeyOnDisk(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().containsKeyOnDisk(arg0);
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
    // public abstract net.sf.ehcache.search.Results
    // net.sf.ehcache.store.Store.executeQuery(net.sf.ehcache.store.StoreQuery) throws
    // net.sf.ehcache.search.SearchException
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Results _ret = this.delegate.executeQuery(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().executeQuery(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().executeQuery(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().executeQuery(arg0);
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
    // public abstract net.sf.ehcache.Element net.sf.ehcache.store.Store.getQuiet(java.lang.Object)
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Element _ret = this.delegate.getQuiet(arg0);
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getQuiet(arg0);
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getQuiet(arg0);
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().getQuiet(arg0);
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
    // public abstract net.sf.ehcache.Status net.sf.ehcache.store.Store.getStatus()
    nonStop.start(toolkitNonStopConfiguration);
    try {
      throwNonStopExceptionWhenClusterNotInit();
      Status _ret = this.delegate.getStatus();
      nonstopObserver.end(NonStopOperationOutcomes.SUCCESS);
      return _ret;
    } catch (NonStopToolkitInstantiationException e) {
      handleNonStopToolkitInstantiationException(e);
      return getTimeoutBehavior().getStatus();
    } catch (NonStopException e) {
      nonstopObserver.end(NonStopOperationOutcomes.TIMEOUT);
      return getTimeoutBehavior().getStatus();
    } catch (RejoinException e) {
      nonstopObserver.end(NonStopOperationOutcomes.REJOIN_TIMEOUT);
      return getTimeoutBehavior().getStatus();
    } finally {
      nonStop.finish();
    }
  }

}

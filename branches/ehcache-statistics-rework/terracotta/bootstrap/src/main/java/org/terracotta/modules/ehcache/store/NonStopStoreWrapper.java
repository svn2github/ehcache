/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
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

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.nonstop.NonStop;
import org.terracotta.toolkit.nonstop.NonStopException;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NonStopStoreWrapper implements TerracottaStore {
  private final TerracottaStore             delegate;
  private final NonStop                     nonStop;
  private final ToolkitNonStopConfiguration toolkitNonStopConfiguration;

  public NonStopStoreWrapper(TerracottaStore delegate, ToolkitInstanceFactory toolkitInstanceFactory,
                             NonstopConfiguration nonStopConfiguration) {
    this.delegate = delegate;
    this.nonStop = toolkitInstanceFactory.getToolkit().getFeature(NonStop.class);
    this.toolkitNonStopConfiguration = new ToolkitNonStopConfiguration(nonStopConfiguration);
  }

  public static void main(String[] args) {
    PrintStream out = System.out;
    Class[] classes = { TerracottaStore.class };

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
        out.println("    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!");
        out.println("      nonStopToolkit.begin(toolkitNonStopConfiguration);");
        out.println("      try {");

        out.print("        ");
        if (m.getReturnType() != Void.TYPE) {
          out.print("return ");
        }
        out.print("this.delegate." + m.getName() + "(");
        for (int i = 0; i < params.length; i++) {
          out.print("arg" + i);
          if (i < params.length - 1) {
            out.print(", ");
          }
        }
        out.println(");");
        out.println("      } catch (NonStopException e) {");
        out.println("        throw new NonStopCacheException(e);");
        out.println("      } finally {");
        out.println("        nonStopToolkit.finish();");
        out.println("      }");
        out.println("}");
        out.println("");
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element unsafeGet(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.unsafeGet(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set getLocalKeys() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.getLocalKeys();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
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
      return this.delegate.getTransactionalMode();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.get(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.put(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.replace(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.replace(arg0, arg1, arg2);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.delegate.putAll(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.remove(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      this.delegate.flush();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.containsKey(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.getSize();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.delegate.removeAll(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.delegate.removeAll();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.removeElement(arg0, arg1);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.putIfAbsent(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getInternalContext() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.getInternalContext();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.hasAbortedSizeOf();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.getOnDiskSize();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyOffHeap(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.containsKeyOffHeap(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.containsKeyInMemory(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      this.delegate.setInMemoryEvictionPolicy(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.executeQuery(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.putWithWriter(arg0, arg1);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      this.delegate.recalculateSize(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.getQuiet(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getInMemorySize() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.getInMemorySize();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.isCacheCoherent();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getOffHeapSizeInBytes() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.getOffHeapSizeInBytes();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getMBean() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.getMBean();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPinned(Object arg0, boolean arg1) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.delegate.setPinned(arg0, arg1);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.getOnDiskSizeInBytes();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeStoreListener(StoreListener arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.delegate.removeStoreListener(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getInMemorySizeInBytes() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.getInMemorySizeInBytes();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPinned(Object arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.isPinned(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.getTerracottaClusteredSize();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.delegate.dispose();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      this.delegate.expireElements();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean bufferFull() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.bufferFull();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.delegate.setNodeCoherent(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.isNodeCoherent();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      this.delegate.addStoreListener(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.isClusterCoherent();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException,
      InterruptedException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.delegate.waitUntilClusterCoherent();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.getInMemoryEvictionPolicy();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.removeWithWriter(arg0, arg1);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.getKeys();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.getStatus();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOffHeapSize() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.getOffHeapSize();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.getSearchAttribute(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      return this.delegate.containsKeyOnDisk(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unpinAll() {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    nonStop.start(toolkitNonStopConfiguration);
    try {
      this.delegate.unpinAll();
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
      this.delegate.setAttributeExtractors(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.getAllQuiet(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
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
    nonStop.start(toolkitNonStopConfiguration);
    try {
      return this.delegate.getAll(arg0);
    } catch (NonStopException e) {
      throw new NonStopCacheException(e);
    } finally {
      nonStop.finish();
    }
  }
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
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
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.terracotta.context.annotations.ContextChild;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.rejoin.RejoinException;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper over ClusteredStore to have custom exception handlers for Store operations.
 */
public class ClusteredSafeStore implements TerracottaStore {
  private static final ClusteredStoreExceptionHandler EXCEPTION_HANDLER = new ClusteredSafeStoreExceptionHandler();

  @ContextChild
  private final TerracottaStore                       delegateClusteredStore;

  public ClusteredSafeStore(final TerracottaStore delegateClusteredStore) {
    this.delegateClusteredStore = delegateClusteredStore;
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

        List<Class> exceptions = new ArrayList<Class>();
        exceptions.add(NonStopException.class);
        exceptions.add(RejoinException.class);
        for (Class e : m.getExceptionTypes()) {
          exceptions.add(e);
        }
        if (exceptions.size() > 0) {
          out.print(" throws ");
        }
        for (int i = 0; i < exceptions.size(); i++) {
          out.print(exceptions.get(i).getSimpleName());
          if (i < exceptions.size() - 1) {
            out.print(", ");
          }
        }

        out.println(" {");
        out.println("    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!");
        out.println("    try {");
        out.print("        ");
        if (m.getReturnType() != Void.TYPE) {
          out.print("return ");
        }
        out.print("this.delegateClusteredStore." + m.getName() + "(");
        for (int i = 0; i < params.length; i++) {
          out.print("arg" + i);
          if (i < params.length - 1) {
            out.print(", ");
          }
        }
        out.println(");");
        if (exceptions.size() > 0) {
          for (int i = 0; i < exceptions.size(); i++) {
            Class e = exceptions.get(i);
            out.println("    } catch(" + e.getSimpleName() + " e) {");
            out.println("      throw e;");
            if (i < exceptions.size() - 1) {
              continue;
            }
          }
        }
        out.println("    } catch (Throwable t) {");
        out.println("        EXCEPTION_HANDLER.handleException(t);");
        out.println("        throw new CacheException(\"Uncaught exception in " + m.getName()
                    + "() - \" + t.getMessage(), t);");
        out.println("    }");
        out.println("}");
        out.println("");
      }
    }
  }

  @Override
  public WriteBehind createWriteBehind() {
    throw new UnsupportedOperationException();
  }

  private static class ClusteredSafeStoreExceptionHandler implements ClusteredStoreExceptionHandler {

    @Override
    public void handleException(Throwable t) {
      if (t.getClass().getSimpleName().equals("TCNotRunningException")) { throw new TerracottaNotRunningException(
                                                                                                                  "Clustered Cache is probably shutdown or Terracotta backend is down.",
                                                                                                                  t); }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element unsafeGet(Object arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.unsafeGet(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in unsafeGet() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set getLocalKeys() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getLocalKeys();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getLocalKeys() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransactionalMode getTransactionalMode() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getTransactionalMode();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getTransactionalMode() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element get(Object arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.get(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in get() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean put(Element arg0) throws NonStopException, RejoinException, CacheException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.put(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (CacheException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in put() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element replace(Element arg0) throws NonStopException, RejoinException,
      NullPointerException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.replace(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (NullPointerException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in replace() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean replace(Element arg0, Element arg1, ElementValueComparator arg2) throws NonStopException,
      RejoinException, NullPointerException, IllegalArgumentException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.replace(arg0, arg1, arg2);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (NullPointerException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in replace() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putAll(Collection arg0) throws NonStopException, RejoinException, CacheException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.putAll(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (CacheException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in putAll() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element remove(Object arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.remove(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in remove() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() throws NonStopException, RejoinException, IOException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.flush();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in flush() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKey(Object arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.containsKey(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in containsKey() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getSize() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getSize();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getSize() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAll() throws NonStopException, RejoinException, CacheException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.removeAll();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (CacheException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in removeAll() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAll(Collection arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.removeAll(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in removeAll() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element removeElement(Element arg0, ElementValueComparator arg1) throws NonStopException,
 RejoinException,
      NullPointerException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.removeElement(arg0, arg1);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (NullPointerException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in removeElement() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element putIfAbsent(Element arg0) throws NonStopException, RejoinException,
      NullPointerException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.putIfAbsent(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (NullPointerException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in putIfAbsent() - " + t.getMessage(), t);
    }
  }

    /**
   * {@inheritDoc}
   */
  @Override
  public void addStoreListener(StoreListener arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.addStoreListener(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in addStoreListener() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeStoreListener(StoreListener arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.removeStoreListener(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in removeStoreListener() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putWithWriter(Element arg0, CacheWriterManager arg1) throws NonStopException,
 RejoinException,
      CacheException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.putWithWriter(arg0, arg1);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (CacheException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in putWithWriter() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element getQuiet(Object arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getQuiet(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getQuiet() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element removeWithWriter(Object arg0, CacheWriterManager arg1) throws NonStopException,
 RejoinException,
      CacheException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.removeWithWriter(arg0, arg1);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (CacheException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in removeWithWriter() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getInMemorySize() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getInMemorySize();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getInMemorySize() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOffHeapSize() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getOffHeapSize();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getOffHeapSize() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOnDiskSize() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getOnDiskSize();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getOnDiskSize() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getTerracottaClusteredSize() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getTerracottaClusteredSize();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getTerracottaClusteredSize() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getInMemorySizeInBytes() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getInMemorySizeInBytes();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getInMemorySizeInBytes() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getOffHeapSizeInBytes() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getOffHeapSizeInBytes();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getOffHeapSizeInBytes() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getOnDiskSizeInBytes() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getOnDiskSizeInBytes();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getOnDiskSizeInBytes() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasAbortedSizeOf() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.hasAbortedSizeOf();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in hasAbortedSizeOf() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyOnDisk(Object arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.containsKeyOnDisk(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in containsKeyOnDisk() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyOffHeap(Object arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.containsKeyOffHeap(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in containsKeyOffHeap() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyInMemory(Object arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.containsKeyInMemory(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in containsKeyInMemory() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void expireElements() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.expireElements();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in expireElements() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean bufferFull() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.bufferFull();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in bufferFull() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Policy getInMemoryEvictionPolicy() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getInMemoryEvictionPolicy();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getInMemoryEvictionPolicy() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setInMemoryEvictionPolicy(Policy arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.setInMemoryEvictionPolicy(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in setInMemoryEvictionPolicy() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getInternalContext() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getInternalContext();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getInternalContext() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCacheCoherent() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.isCacheCoherent();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in isCacheCoherent() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isClusterCoherent() throws NonStopException, RejoinException,
      TerracottaNotRunningException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.isClusterCoherent();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (TerracottaNotRunningException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in isClusterCoherent() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isNodeCoherent() throws NonStopException, RejoinException,
      TerracottaNotRunningException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.isNodeCoherent();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (TerracottaNotRunningException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in isNodeCoherent() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setNodeCoherent(boolean arg0) throws NonStopException, RejoinException,
      UnsupportedOperationException, TerracottaNotRunningException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.setNodeCoherent(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (UnsupportedOperationException e) {
      throw e;
    } catch (TerracottaNotRunningException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in setNodeCoherent() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void waitUntilClusterCoherent() throws NonStopException, RejoinException,
      UnsupportedOperationException, TerracottaNotRunningException, InterruptedException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.waitUntilClusterCoherent();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (UnsupportedOperationException e) {
      throw e;
    } catch (TerracottaNotRunningException e) {
      throw e;
    } catch (InterruptedException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in waitUntilClusterCoherent() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getMBean() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getMBean();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getMBean() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAttributeExtractors(Map arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.setAttributeExtractors(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in setAttributeExtractors() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Results executeQuery(StoreQuery arg0) throws NonStopException, RejoinException,
      SearchException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.executeQuery(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (SearchException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in executeQuery() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Attribute getSearchAttribute(String arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getSearchAttribute(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getSearchAttribute() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map getAllQuiet(Collection arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getAllQuiet(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getAllQuiet() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map getAll(Collection arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getAll(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getAll() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.dispose();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in dispose() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List getKeys() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getKeys();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getKeys() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Status getStatus() throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      return this.delegateClusteredStore.getStatus();
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in getStatus() - " + t.getMessage(), t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recalculateSize(Object arg0) throws NonStopException, RejoinException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    try {
      this.delegateClusteredStore.recalculateSize(arg0);
    } catch (NonStopException e) {
      throw e;
    } catch (RejoinException e) {
      throw e;
    } catch (Throwable t) {
      EXCEPTION_HANDLER.handleException(t);
      throw new CacheException("Uncaught exception in recalculateSize() - " + t.getMessage(), t);
    }
  }
}

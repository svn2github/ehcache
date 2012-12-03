/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.concurrency;

import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.store.ToolkitNonStopConfiguration;
import org.terracotta.toolkit.NonStopToolkit;
import org.terracotta.toolkit.nonstop.NonStopException;

import java.io.PrintStream;
import java.lang.reflect.Method;

public class NonStopSyncWrapper implements Sync {
  private final Sync                        delegate;
  private final NonStopToolkit              nonStopToolkit;
  private final ToolkitNonStopConfiguration toolkitNonStopConfiguration;

  public NonStopSyncWrapper(Sync delegate, ToolkitInstanceFactory toolkitInstanceFactory,
                            NonstopConfiguration nonStopConfiguration) {
    this.delegate = delegate;
    this.nonStopToolkit = toolkitInstanceFactory.getToolkit().asNonStopToolkit();
    this.toolkitNonStopConfiguration = new ToolkitNonStopConfiguration(nonStopConfiguration);
  }

  public static void main(String[] args) {
    PrintStream out = System.out;
    Class[] classes = { Sync.class };

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

        out.println("    if (!toolkitNonStopConfiguration.isEnabled()) {");
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

        out.println("    } else {");
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
        out.println(" }");
        out.println("");
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void lock(LockType arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    if (!toolkitNonStopConfiguration.isEnabled()) {
      this.delegate.lock(arg0);
    } else {
      nonStopToolkit.begin(toolkitNonStopConfiguration);
      try {
        this.delegate.lock(arg0);
      } catch (NonStopException e) {
        throw new NonStopCacheException(e);
      } finally {
        nonStopToolkit.finish();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unlock(LockType arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    if (!toolkitNonStopConfiguration.isEnabled()) {
      this.delegate.unlock(arg0);
    } else {
      nonStopToolkit.begin(toolkitNonStopConfiguration);
      try {
        this.delegate.unlock(arg0);
      } catch (NonStopException e) {
        throw new NonStopCacheException(e);
      } finally {
        nonStopToolkit.finish();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean tryLock(LockType arg0, long arg1) throws InterruptedException {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    if (!toolkitNonStopConfiguration.isEnabled()) {
      return this.delegate.tryLock(arg0, arg1);
    } else {
      nonStopToolkit.begin(toolkitNonStopConfiguration);
      try {
        return this.delegate.tryLock(arg0, arg1);
      } catch (NonStopException e) {
        throw new NonStopCacheException(e);
      } finally {
        nonStopToolkit.finish();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isHeldByCurrentThread(LockType arg0) {
    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
    if (!toolkitNonStopConfiguration.isEnabled()) {
      return this.delegate.isHeldByCurrentThread(arg0);
    } else {
      nonStopToolkit.begin(toolkitNonStopConfiguration);
      try {
        return this.delegate.isHeldByCurrentThread(arg0);
      } catch (NonStopException e) {
        throw new NonStopCacheException(e);
      } finally {
        nonStopToolkit.finish();
      }
    }
  }

}

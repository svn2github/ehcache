/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cdennis
 */
public class Pounder extends Runner {

  private static final Logger LOG = LoggerFactory.getLogger(Pounder.class);

  private final Class<?> testClass;
  private final long duration;
  
  public Pounder(Class<?> testClass) throws InitializationError {
    this.testClass = testClass;
    PoundFor durationAnno = testClass.getAnnotation(PoundFor.class);
    if (durationAnno == null) {
      duration = Long.MAX_VALUE;
      LOG.info("Pounding on '" + testClass.getSimpleName() + "' until failure.");
    } else {
      duration = durationAnno.unit().toNanos(durationAnno.time());
      LOG.info("Pounding on '" + testClass.getSimpleName() + "' for  " + durationAnno.time() + " " + durationAnno.unit().name().toLowerCase() + ".");
    }
  }
  
  @Override
  public Description getDescription() {
    return Description.createSuiteDescription(testClass);
  }

  @Override
  public void run(RunNotifier rn) {
    Listener listener = new Listener();
    rn.addListener(listener);
    try {
      long start = System.nanoTime();
      long count = 0;
      do {
        try {
          LOG.info("Pounding Run " + (count + 1));
          new JUnit4(testClass).run(rn);
          count++;
        } catch (InitializationError ex) {
          rn.fireTestFailure(new Failure(getDescription(), ex));
        }
      } while((System.nanoTime() - start < duration) && !listener.hasFailed()) ;
    } finally {
      rn.removeListener(listener);
    }
  }
  
  static class Listener extends RunListener {

    private boolean failed;

    @Override
    public void testFailure(Failure failure) throws Exception {
      failed = true;
    }

    
    private boolean hasFailed() {
      return failed;
    }
    
  }
  
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface PoundFor {
    long time();
    TimeUnit unit();
  }
}

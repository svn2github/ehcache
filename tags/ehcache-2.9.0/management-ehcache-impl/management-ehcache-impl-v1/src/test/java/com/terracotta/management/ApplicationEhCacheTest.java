package com.terracotta.management;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
/**
 * @author: Anthony Dahanne
 */
public class ApplicationEhCacheTest extends JerseyApplicationTestCommon {

  @Test
  public void testGetClasses() throws Exception {
    ApplicationEhCacheV1 applicationEhCache = new ApplicationEhCacheV1();
    Set<Class<?>> filteredApplicationClasses = filterClassesFromJaxRSPackages(applicationEhCache.getRestResourceClasses());

    Set<Class<?>> annotatedClasses = annotatedClassesFound();
    Set<Class<?>> classesToIgnoreDuringComparison = new HashSet<Class<?>>();

    if (filteredApplicationClasses.size() > annotatedClasses.size()) {
      for (Class<?> applicationClass : filteredApplicationClasses) {
        if(!annotatedClasses.contains(applicationClass)) {
          fail("While scanning the classpath, we could not find " + applicationClass);
        }
      }
    } else {
      for (Class<?> annotatedClass : annotatedClasses) {
        if (!filteredApplicationClasses.contains(annotatedClass)) {
          fail("Should  " + annotatedClass + " be added to ApplicationEhCacheV1 ?");
        }
      }
    }
    filteredApplicationClasses.removeAll(classesToIgnoreDuringComparison);
    Assert.assertThat(annotatedClasses, equalTo(filteredApplicationClasses));
  }

}
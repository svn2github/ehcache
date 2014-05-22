package com.terracotta.management;

import static org.hamcrest.Matchers.equalTo;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * @author: Anthony Dahanne
 */
public class ApplicationEhCacheV2Test extends ApplicationEhCacheTestCommon {

  @Test
  public void testGetClasses() throws Exception {
    ApplicationEhCacheV2 applicationEhCache = new ApplicationEhCacheV2();
    Set<Class<?>> applicationClasses = applicationEhCache.getRestResourceClasses();
    Set<Class<?>> annotatedClasses = annotatedClassesFound();
    Assert.assertThat(annotatedClasses, equalTo(applicationClasses));
  }

}

package com.terracotta.management;

import static org.hamcrest.Matchers.equalTo;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
/**
 * @author: Anthony Dahanne
 */
public class ApplicationEhCacheTest extends JerseyApplicationTestCommon {

  @Test
  public void testGetClasses() throws Exception {
    ApplicationEhCacheV1 applicationEhCache = new ApplicationEhCacheV1();
    Set<Class<?>> applicationClasses = applicationEhCache.getRestResourceClasses();
    Set<Class<?>> annotatedClasses = annotatedClassesFound();
    Assert.assertThat(annotatedClasses, equalTo(applicationClasses));
  }

}
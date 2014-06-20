/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services.validator;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.junit.*;
import static org.mockito.Mockito.*;

/**
 * 
 * @author Anthony Dahanne
 *
 */
public class AbstractEhcacheRequestValidatorTest {


  @Test
  public void validateCacheManagerRequestSegmentTest_v1() {
    AbstractEhcacheRequestValidator abstractEhcacheRequestValidator = new AbstractEhcacheRequestValidatorExtension();

    List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    PathSegment pathSegmentAgents = org.mockito.Mockito.mock(PathSegment.class);
    when(pathSegmentAgents.getPath()).thenReturn("agents");
    PathSegment pathSegmentCacheManagers = org.mockito.Mockito.mock(PathSegment.class);
    when(pathSegmentCacheManagers.getPath()).thenReturn("cacheManagers");

    pathSegments.add(pathSegmentAgents);
    pathSegments.add(pathSegmentCacheManagers);

    Assert.assertEquals(pathSegmentCacheManagers,
        abstractEhcacheRequestValidator.getCacheManagerPathSegmentAccordingToVersion(pathSegments));

  }

  @Test
  public void validateCacheManagerRequestSegmentTest_v2() {
    AbstractEhcacheRequestValidator abstractEhcacheRequestValidator = new AbstractEhcacheRequestValidatorExtension();

    List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    PathSegment pathSegmentVersion = org.mockito.Mockito.mock(PathSegment.class);
    when(pathSegmentVersion.getPath()).thenReturn("v2");
    PathSegment pathSegmentAgents = org.mockito.Mockito.mock(PathSegment.class);
    when(pathSegmentAgents.getPath()).thenReturn("agents");
    PathSegment pathSegmentCacheManagers = org.mockito.Mockito.mock(PathSegment.class);
    when(pathSegmentCacheManagers.getPath()).thenReturn("cacheManagers");

    pathSegments.add(pathSegmentVersion);
    pathSegments.add(pathSegmentAgents);
    pathSegments.add(pathSegmentCacheManagers);

    Assert.assertEquals(pathSegmentCacheManagers,
        abstractEhcacheRequestValidator.getCacheManagerPathSegmentAccordingToVersion(pathSegments));

  }

  private final class AbstractEhcacheRequestValidatorExtension extends AbstractEhcacheRequestValidator {
    @Override
    public void validateSafe(UriInfo info) {
      // TODO Auto-generated method stub

    }

    @Override
    protected void validateAgentSegment(List<PathSegment> pathSegments) {
      // TODO Auto-generated method stub

    }
  }

}

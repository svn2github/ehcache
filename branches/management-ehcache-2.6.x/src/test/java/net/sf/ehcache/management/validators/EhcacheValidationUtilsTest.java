/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.validators;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import junit.framework.Assert;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.expect;

/**
 * @author brandony
 */
public class EhcacheValidationUtilsTest extends EasyMockSupport {

  @Test
  public void testValidateUnsafeCacheManagerRequest() {
    UriInfo info = createMock(UriInfo.class);
    List<PathSegment> pss = new ArrayList<PathSegment>();
    expect(info.getPathSegments()).andReturn(pss);

    PathSegment ps0 = createMock(PathSegment.class);
    pss.add(ps0);

    PathSegment ps1 = createMock(PathSegment.class);
    pss.add(ps1);
    MultivaluedMap<String, String> mvm = new MultivaluedMapImpl();
    mvm.putSingle("names", "foo");
    expect(ps1.getMatrixParameters()).andReturn(mvm);

    // Perfect amount
    replayAll();
    try {
      EhcacheValidationUtils.validateUnsafeCacheManagerRequest(info);
    } catch (WebApplicationException e) {
      Assert.fail("Valid request info has been rejected!");
    }
    verifyAll();

    // Too many
    mvm.putSingle("names", "foo,bar,baz");

    resetAll();
    expect(info.getPathSegments()).andReturn(pss);
    expect(ps1.getMatrixParameters()).andReturn(mvm);

    replayAll();
    try {
      EhcacheValidationUtils.validateUnsafeCacheManagerRequest(info);
      Assert.fail("Valid request info has not been rejected as expected!");
    } catch (WebApplicationException e) {
      Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
    }
    verifyAll();

    // Names are null
    mvm.putSingle("names", null);

    resetAll();
    expect(info.getPathSegments()).andReturn(pss);
    expect(ps1.getMatrixParameters()).andReturn(mvm);

    replayAll();
    try {
      EhcacheValidationUtils.validateUnsafeCacheManagerRequest(info);
      Assert.fail("Valid request info has not been rejected as expected!");
    } catch (WebApplicationException e) {
      Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
    }
    verifyAll();

    // Names dont exist
    mvm.remove("names");

    resetAll();
    expect(info.getPathSegments()).andReturn(pss);
    expect(ps1.getMatrixParameters()).andReturn(mvm);

    replayAll();
    try {
      EhcacheValidationUtils.validateUnsafeCacheManagerRequest(info);
      Assert.fail("Valid request info has not been rejected as expected!");
    } catch (WebApplicationException e) {
      Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
    }
    verifyAll();
  }

  @Test
  public void testValidateUnsafeCacheRequest() {
    UriInfo info = createMock(UriInfo.class);
    List<PathSegment> pss = new ArrayList<PathSegment>();
    expect(info.getPathSegments()).andReturn(pss);

    PathSegment ps0 = createMock(PathSegment.class);
    pss.add(ps0);

    PathSegment ps1 = createMock(PathSegment.class);
    pss.add(ps1);

    PathSegment ps2 = createMock(PathSegment.class);
    pss.add(ps2);
    MultivaluedMap<String, String> mvm = new MultivaluedMapImpl();
    mvm.putSingle("names", "foo");
    expect(ps2.getMatrixParameters()).andReturn(mvm);

    // Perfect amount
    replayAll();
    try {
      EhcacheValidationUtils.validateUnsafeCacheRequest(info);
    } catch (WebApplicationException e) {
      Assert.fail("Valid request info has been rejected!");
    }
    verifyAll();

    // Too many
    mvm.putSingle("names", "foo,bar,baz");

    resetAll();
    expect(info.getPathSegments()).andReturn(pss);
    expect(ps2.getMatrixParameters()).andReturn(mvm);

    replayAll();
    try {
      EhcacheValidationUtils.validateUnsafeCacheRequest(info);
      Assert.fail("Valid request info has not been rejected as expected!");
    } catch (WebApplicationException e) {
      Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
    }
    verifyAll();

    // Names are null
    mvm.putSingle("names", null);

    resetAll();
    expect(info.getPathSegments()).andReturn(pss);
    expect(ps2.getMatrixParameters()).andReturn(mvm);

    replayAll();
    try {
      EhcacheValidationUtils.validateUnsafeCacheRequest(info);
      Assert.fail("Valid request info has not been rejected as expected!");
    } catch (WebApplicationException e) {
      Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
    }
    verifyAll();

    // Names dont exist
    mvm.remove("names");

    resetAll();
    expect(info.getPathSegments()).andReturn(pss);
    expect(ps2.getMatrixParameters()).andReturn(mvm);

    replayAll();
    try {
      EhcacheValidationUtils.validateUnsafeCacheRequest(info);
      Assert.fail("Valid request info has not been rejected as expected!");
    } catch (WebApplicationException e) {
      Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
    }
    verifyAll();
  }
}

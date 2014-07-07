package net.sf.ehcache.management.service.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.AbstractManagementServer;
import net.sf.ehcache.management.service.ManagementServerLifecycle;

import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.embedded.StandaloneServer;

@RunWith(PowerMockRunner.class)
@PrepareForTest(StandaloneServer.class)
/**
 * 
 * THis test verifies basic interaction between AbstractManagementServer and its dependencies
 * PowerMock is used to mock StandaloneServer, which is a final class.
 * 
 * @author Anthony Dahanne
 *
 */
public class AbstractManagementServerTest {

  private CacheManager cacheManager;

  @Before
  public void setUp() throws Exception {
    ServiceLocator.unload();
    cacheManager = createMock(CacheManager.class);
  }

  @After
  public void tearDown() {
    cacheManager.shutdown();
  }

  @Test
  /**
   * Verifies that managementServerstart() calls server.start()
   * @throws Exception
   */
  public void startTest() throws Exception {
    StandaloneServer serverMock = PowerMock.createMock(StandaloneServer.class);
    ManagementServer managementServer = new ManagementServer(serverMock, null);
    serverMock.start();
    PowerMock.expectLastCall().andAnswer(new NullAnswer<Object>());
    PowerMock.replay(serverMock);
    managementServer.start();
    PowerMock.verify(serverMock);
  }

  @Test(expected = CacheException.class)
  /**
   * Verifies that managementServer.start() calls server.start() and rethrows the exception
   * @throws Exception
   */
  public void startTestException() throws Exception {
    StandaloneServer serverMock = PowerMock.createMock(StandaloneServer.class);
    ManagementServerLifecycle sampleRepositoryServiceMock = createMock(ManagementServerLifecycle.class);
    ManagementServer managementServer = new ManagementServer(serverMock, sampleRepositoryServiceMock);
    serverMock.start();
    PowerMock.expectLastCall().andAnswer(new ExceptionAnswer<Object>());
    sampleRepositoryServiceMock.dispose();
    expectLastCall().andAnswer(new NullAnswer<Object>());
    PowerMock.replay(serverMock);
    managementServer.start();
    PowerMock.verify(serverMock);
  }

  @Test
  public void stopTest() throws Exception {
    StandaloneServer serverMock = PowerMock.createMock(StandaloneServer.class);
    ManagementServerLifecycle sampleRepositoryServiceMock = createMock(ManagementServerLifecycle.class);
    ManagementServer managementServer = new ManagementServer(serverMock, sampleRepositoryServiceMock);

    serverMock.stop();
    PowerMock.expectLastCall().andAnswer(new NullAnswer<Object>());
    sampleRepositoryServiceMock.dispose();
    expectLastCall().andAnswer(new NullAnswer<Object>());

    PowerMock.replay(serverMock);
    replay(sampleRepositoryServiceMock);


    //stop is also calling ServiceLocator.unload() ; to make this is called, let's load it first
    //and assert it is unloaded after stop()
    managementServer.loadEmbeddedAgentServiceLocatorWithStringClass();
    assertNotNull(ServiceLocator.locate(String.class));

    managementServer.stop();

    IllegalStateException exceptionThrown = null;
    try {
      // since the servicelocator is unloaded, it should throw an exception
      ServiceLocator.locate(String.class);
    } catch (IllegalStateException e) {
      exceptionThrown = e;
    }
    assertNotNull(exceptionThrown);

    PowerMock.verify(serverMock);
    verify(sampleRepositoryServiceMock);
  }

  @Test
  public void registerTest() {
    ManagementServerLifecycle sampleRepositoryServiceMock = createMock(ManagementServerLifecycle.class);
    ManagementServer managementServer = new ManagementServer(null, sampleRepositoryServiceMock);
    sampleRepositoryServiceMock.register(cacheManager);
    expectLastCall().andAnswer(new NullAnswer<Object>());
    replay(sampleRepositoryServiceMock);
    managementServer.register(cacheManager);
    verify(sampleRepositoryServiceMock);
  }


  @Test
  public void unregisterTest() {
    ManagementServerLifecycle sampleRepositoryServiceMock = createMock(ManagementServerLifecycle.class);
    ManagementServer managementServer = new ManagementServer(null, sampleRepositoryServiceMock);
    sampleRepositoryServiceMock.unregister(cacheManager);
    expectLastCall().andAnswer(new NullAnswer<Object>());
    replay(sampleRepositoryServiceMock);
    managementServer.unregister(cacheManager);
    verify(sampleRepositoryServiceMock);
  }

  @Test
  public void hasRegisteredTest() {
    ManagementServerLifecycle sampleRepositoryServiceMock = createMock(ManagementServerLifecycle.class);
    ManagementServer managementServer = new ManagementServer(null, sampleRepositoryServiceMock);
    expect(sampleRepositoryServiceMock.hasRegistered()).andReturn(Boolean.TRUE);
    replay(sampleRepositoryServiceMock);
    assertTrue(managementServer.hasRegistered());
    verify(sampleRepositoryServiceMock);
  }

  class NullAnswer<Object> implements IAnswer<Object> {
    @Override
    public Object answer() throws Throwable {
      return null;
    }
  }

  class ExceptionAnswer<Object> implements IAnswer<Object> {
    @Override
    public Object answer() throws Throwable {
      throw new NullPointerException();
    }
  }

  class ManagementServer extends AbstractManagementServer {

    ManagementServer(StandaloneServer server, ManagementServerLifecycle repositoryService) {
      this.standaloneServer = server;
      this.managementServerLifecycles.add(repositoryService);
    }

    protected void loadEmbeddedAgentServiceLocatorWithStringClass() {
      ServiceLocator locator = new ServiceLocator();
      locator.loadService(String.class, new String());
      // service locator is initialized with 1 service : String
      ServiceLocator.load(locator);
    }

    @Override
    public void initialize(ManagementRESTServiceConfiguration configuration) {
    }

    @Override
    public void registerClusterRemoteEndpoint(String clientUUID) {
    }

    @Override
    public void unregisterClusterRemoteEndpoint(String clientUUID) {
    }
  }


}

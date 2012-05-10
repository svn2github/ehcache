package net.sf.ehcache.tests;

import com.tc.test.config.model.TestConfig;
import com.terracotta.management.embedded.StandaloneServer;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.management.resource.services.CacheManagersResourceService;
import net.sf.ehcache.management.resource.services.CacheManagersResourceServiceImpl;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.terracotta.express.ClientFactory;
import org.terracotta.tests.base.AbstractTestBase;

import java.io.IOException;

import static org.terracotta.test.util.TestBaseUtil.jarFor;

/**
 * @author Ludovic Orban
 */
public abstract class AbstractEhcacheManagementTestBase extends AbstractTestBase {
  public AbstractEhcacheManagementTestBase(TestConfig testConfig) {
    super(testConfig);
  }

  @Override
  protected String createClassPath(final Class client) throws IOException {
    String ehcacheManagement1 = jarFor(StandaloneServer.class);
    String ehcacheManagement2 = jarFor(CacheManagersResourceService.class);
    String ehcacheManagement3 = jarFor(CacheManagersResourceServiceImpl.class);

    String jetty1 = jarFor(org.eclipse.jetty.server.Handler.class);
    String jetty2 = jarFor(org.eclipse.jetty.servlet.DefaultServlet.class);
    String jetty3 = jarFor(org.eclipse.jetty.util.IO.class);
    String jetty4 = jarFor(org.eclipse.jetty.security.DefaultUserIdentity.class);
    String jetty5 = jarFor(org.eclipse.jetty.http.HttpCookie.class);
    String jetty6 = jarFor(org.eclipse.jetty.io.EndPoint.class);
    String jetty7 = jarFor(org.eclipse.jetty.continuation.Continuation.class);
    String servlet = jarFor(javax.servlet.Servlet.class);

    String jersey1 = jarFor(com.sun.jersey.core.header.ContentDisposition.class);
    String jersey2 = jarFor(com.sun.jersey.api.json.JSONConfiguration.class);
    String jersey3 = jarFor(com.sun.jersey.server.impl.BuildId.class);
    String jersey4 = jarFor(com.sun.jersey.spi.container.servlet.ServletContainer.class);

    String jettison = jarFor(org.codehaus.jettison.Node.class);
    String jackson1 = jarFor(org.codehaus.jackson.JsonNode.class);
    String jackson2 = jarFor(org.codehaus.jackson.jaxrs.Annotations.class);
    String jackson3 = jarFor(org.codehaus.jackson.map.AbstractTypeResolver.class);
    String jackson4 = jarFor(org.codehaus.jackson.xc.DataHandlerJsonDeserializer.class);

    String restAssured = jarFor(com.jayway.restassured.RestAssured.class);
    String hamcrest1 = jarFor(org.hamcrest.Matcher.class);
    String hamcrest2 = jarFor(org.hamcrest.Matchers.class);
    String httpClient1 = jarFor(org.apache.http.client.HttpClient.class);
    String httpClient2 = jarFor(org.apache.http.HttpRequest.class);
    String commonsLang = jarFor(org.apache.commons.lang.StringUtils.class);
    String commonsLogging = jarFor(org.apache.commons.logging.Log.class);
    String commonsCollections = jarFor(org.apache.commons.collections.iterators.ArrayIterator.class);
    String commonsBeanUtils = jarFor(org.apache.commons.beanutils.DynaBean.class);
    String json = jarFor(net.sf.json.JSONObject.class);
    String xmlResolver = jarFor(org.apache.xml.resolver.CatalogManager.class);
    String ezMorph = jarFor(net.sf.ezmorph.Morpher.class);
    String antlr = jarFor(antlr.collections.AST.class);
    String asm = jarFor(org.objectweb.asm.Opcodes.class);

    String groovy1 = jarFor(groovy.lang.GroovyObject.class);
    String groovy2 = jarFor(groovyx.net.http.ContentType.class);
    
    String spring1 = jarFor(org.springframework.context.ApplicationContext.class);
    String spring2 = jarFor(org.springframework.beans.factory.ListableBeanFactory.class);
    String spring3 = jarFor(org.springframework.core.io.support.ResourcePatternResolver.class);
    String spring4 = jarFor(com.sun.jersey.spi.spring.container.servlet.SpringServlet.class);
    String spring5 = jarFor(org.springframework.web.context.ConfigurableWebApplicationContext.class);
    String spring6 = jarFor(org.springframework.asm.ClassVisitor.class);
    String spring7 = jarFor(org.springframework.expression.PropertyAccessor.class);
    String spring8 = jarFor(org.springframework.aop.support.annotation.AnnotationMethodMatcher.class);
    
    String aop = jarFor(org.aopalliance.intercept.ConstructorInterceptor.class);
    
    String inject = jarFor(javax.inject.Inject.class);

    Class<?> aClass;
    try {
      aClass = Class.forName("net.sf.ehcache.terracotta.ExpressEnterpriseTerracottaClusteredInstanceFactory");
    } catch (ClassNotFoundException e) {
      try {
        aClass = Class.forName("net.sf.ehcache.terracotta.StandaloneTerracottaClusteredInstanceFactory");
      } catch (ClassNotFoundException e1) {
        throw new RuntimeException("ehcache[-ee].jar is missing from the classpath");
      }
    }
    String clusteredStore = jarFor(aClass);


    return makeClasspath(
        ehcacheManagement1, ehcacheManagement2, ehcacheManagement3,
        jetty1, jetty2, jetty3, jetty4, jetty5, jetty6, jetty7, servlet,
        jersey1, jersey2, jersey3, jersey4,
        jettison, jackson1, jackson2, jackson3, jackson4,
        restAssured, hamcrest1, hamcrest2, httpClient1, httpClient2, commonsLang, commonsLogging, commonsBeanUtils,
        commonsCollections, json, xmlResolver, ezMorph, antlr, asm,
        groovy1, groovy2,
        spring1, spring2, spring3, spring4, spring5, spring6, spring7, spring8,
        aop,
        inject,

        jarFor(ClientFactory.class),
        jarFor(AbstractEhcacheManagementTestBase.class),
        jarFor(LoggerFactory.class),
        jarFor(Ehcache.class),
        clusteredStore,
        jarFor(org.apache.log4j.Logger.class),
        jarFor(com.jayway.restassured.RestAssured.class),
        jarFor(StaticLoggerBinder.class)
    );
  }
}

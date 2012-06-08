/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.jaxrs;

import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * A {@code ContextResolver} that provides special serialization for {@link AgentEntity} objects to reflect the correct
 * attribute name describing the root {@link Representable} objects for this embedded agent.
 *
 * @author brandony
 * 
 */
@Provider
public final class EhcacheObjectMapperProvider implements ContextResolver<ObjectMapper> {
  private static final String ROOT_METHOD_NAME = "getRootRepresentables";

  private static final String EHCACHE_ROOT_NAME = "cacheManagers";

  private final ObjectMapper om;

  public EhcacheObjectMapperProvider() {
    ObjectMapper objectMapper = new ObjectMapper();
    SerializationConfig dflt = objectMapper.getSerializationConfig();
    objectMapper.setSerializationConfig(dflt.withPropertyNamingStrategy(new PropertyNamingStrategy() {

      /**
       * @see org.codehaus.jackson.map.PropertyNamingStrategy#nameForGetterMethod(org.codehaus.jackson.map.MapperConfig,
       *      org.codehaus.jackson.map.introspect.AnnotatedMethod, java.lang.String)
       */
      @Override
      public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
        if (ROOT_METHOD_NAME.equals(method.getName()))
          return EHCACHE_ROOT_NAME;
        else
          return super.nameForGetterMethod(config, method, defaultName);
      }
    }));

    this.om = objectMapper;
  }

  /**
   * @see javax.ws.rs.ext.ContextResolver#getContext(java.lang.Class)
   */
  public ObjectMapper getContext(Class<?> arg0) {
    return om;
  }
}

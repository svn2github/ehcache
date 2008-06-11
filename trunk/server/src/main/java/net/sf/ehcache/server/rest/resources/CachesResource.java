/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.server.rest.resources;

import net.sf.ehcache.server.jaxb.Cache;
import net.sf.ehcache.server.jaxb.Caches;
import net.sf.ehcache.server.ServerContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A CacheManagerResource which permits the following operations:
 * <p/>
 * <code>
 * GET /
 * <p/>
 * Lists the Caches in the CacheManager.
 * </code>
 * <p/>
 * e.g. <code>http://localhost:9998/ehcache/</code>
 *
 * @author Greg Luck
 * @version $Id$
 */
//
@Path("/rest/")
@ProduceMime("application/xml")
public class CachesResource {

    private static final Logger LOG = Logger.getLogger(CachesResource.class.getName());

    /**
     * The full URI for the resource.
     * <p/>
     * e.g. <code>//http://localhost:9998/ehcache/testCache</code>
     */
    @Context
    private UriInfo uriInfo;

    /**
     * The HTTP request
     */
    @Context
    private Request request;


    /**
     * Routes the request to a {@link CacheResource} if the path is <code>/ehcache/{cache}</code>
     * @param cache
     * @return
     */
    @Path("{cache}")
    public CacheResource getCacheResource(@PathParam("cache") String cache) {
        return new CacheResource(uriInfo, request, cache);
    }

    /**
     * GET method implementation. Lists the available caches
     * @return
     */
    @GET
    public Caches getCaches() {
        LOG.info("GET Caches");

        String[] cacheNames = ServerContext.getCacheManager().getCacheNames();

        List<Cache> cacheList = new ArrayList<Cache>();

        for (String cacheName : cacheNames) {
            URI cacheUri = uriInfo.getAbsolutePathBuilder().path(cacheName).build().normalize();
            Cache cache = new Cache(cacheName, cacheUri.toString());
            cacheList.add(cache);
        }

        return new Caches(cacheList);
    }

}

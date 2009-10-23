/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.server.jaxb.Cache;
import net.sf.ehcache.server.jaxb.Statistics;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A resource for a Cache.
 * <p/>
 * A Cache Resource permites the following operations:
 * <p/>
 * <code>OPTIONS ehcache/{cache}</code>
 * <p/>
 * Lists the methods supported by the Cache resource
 * <p/>
 * <code>GET ehcache/{cache}</code>
 * <p/>
 * Lists the elements in the cache.
 * <p/>
 * <code>PUT ehcache/{cache}</code>
 * <p/>
 * Creates a Cache using the defaultCache configuration.
 * <p/>
 * <code>DELETE ehcache/{cache}</code>
 * <p/>
 * Deletes the Cache.
 *
 * @author Greg Luck
 */
@Produces("application/xml")
public class CacheResource {

    private static final Logger LOG = Logger.getLogger(CacheResource.class.getName());

    private static final CacheManager MANAGER;

    static {
        MANAGER = CacheManager.getInstance();
    }


    /**
     * The full URI of the resource
     */
    @Context
    private UriInfo uriInfo;

    /**
     * The request
     */
    @Context
    private Request request;

    /**
     * The name of the cache
     */
    private String cache;

    /**
     * Full constructor
     *
     * @param uriInfo
     * @param request
     * @param cache
     */
    CacheResource(UriInfo uriInfo, Request request, String cache) {
        this.uriInfo = uriInfo;
        this.request = request;
        this.cache = cache;
    }


    /**
     * HEAD method implementation
     *
     * @return
     */
    @HEAD
    public Response getCacheHeader() {
        LOG.log(Level.FINE, "HEAD Cache {}" + cache);

        net.sf.ehcache.Cache ehcache = MANAGER.getCache(this.cache);
        if (ehcache == null) {
            throw new NotFoundException("Cache not found");
        }
        return Response.ok().build();
    }

    /**
     * GET method implementation
     *
     * @return
     */
    @GET
    public Cache getCache() {
        LOG.log(Level.FINE, "GET Cache {}" + cache);

        net.sf.ehcache.Cache ehcache = MANAGER.getCache(cache);
        if (ehcache == null) {
            throw new NotFoundException("Cache not found");
        }

        //The REST API has extra information encoded in the String representation.
        String cacheAsString = ehcache.toString();
        cacheAsString = cacheAsString.substring(0, cacheAsString.length() - 1);
        cacheAsString = cacheAsString + "size = " + ehcache.getSize() + " ]";

        return new Cache(ehcache.getName(), uriInfo.getAbsolutePath().toString(), cacheAsString,
                new Statistics(ehcache.getStatistics()), ehcache.getCacheConfiguration());
    }

    /**
     * PUT method implementation
     *
     * @return
     */
    @PUT
    public Response putCache() {
        LOG.log(Level.FINE, "PUT Cache {}" + cache);

        Response response;

        net.sf.ehcache.Cache ehcache = MANAGER.getCache(cache);
        if (ehcache == null) {
            CacheManager.getInstance().addCache(cache);
            URI uri = uriInfo.getAbsolutePath();
            response = Response.created(uri).build();
            LOG.log(Level.FINE, "Created Cache {}" + cache);
        } else {
            throw new ConflictException("Cache already exists " + cache);
        }
        return response;
    }

    /**
     * DELETE method implementation
     *
     * @return
     */
    @DELETE
    public Response deleteCache() {
        LOG.log(Level.FINE, "DELETE Cache {}" + cache);
        net.sf.ehcache.Cache ehcache = MANAGER.getCache(cache);
        Response response;
        if (ehcache == null) {
            throw new NotFoundException("Cache not found " + cache);
        } else {
            CacheManager.getInstance().removeCache(cache);
            response = Response.ok().build();
        }
        return response;
    }


    /**
     * Routes the request to an {@link ElementResource}
     * <p/>
     * Any extra / are still treated as an element key i.e. limited = false as their is nothing
     * else after element in the URI template.
     *
     * @param element
     * @return
     */
    @Path(value = "{element}")
    public ElementResource getElementResource(@PathParam("element") String element) {
        return new ElementResource(uriInfo, request, cache, element);
    }

}

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

import com.sun.jersey.api.NotFoundException;
import net.sf.ehcache.server.rest.Cache;
import net.sf.ehcache.server.Server;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
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
 * @version $Id$
 */
@ProduceMime("application/xml")
public class CacheResource {

    private static final Logger LOG = Logger.getLogger(CacheResource.class.getName());

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
     * GET method implementation
     */
    @GET
    public Cache getCache() {
        LOG.info("GET Cache " + this.cache);

        net.sf.ehcache.Cache ehcache = Server.getCacheManager().getCache(this.cache);
        if (ehcache == null) {
            throw new NotFoundException("Cache not found");
        }
        return new Cache(ehcache.getName(), uriInfo.getAbsolutePath().toString());
    }

    /**
     * PUT method implementation
     */
    @PUT
    public Response putCache() {
        LOG.info("PUT Cache " + cache);

        URI uri = uriInfo.getAbsolutePath();
        Cache c = new Cache(cache, uri.toString());

        Response response;

        net.sf.ehcache.Cache ehcache = Server.getCacheManager().getCache(cache);
        if (ehcache == null) {
            Server.getCacheManager().addCache(cache);
            response = Response.created(uri).build();
            LOG.info("Created Cache " + cache);
        } else {
            LOG.info("Cache already exists" + cache);
            response = Response.noContent().build();
        }
        return response;
    }

    /**
     * DELETE method implementation
     */
    @DELETE
    public void deleteCache() {
        LOG.info("DELETE Cache " + cache);
        net.sf.ehcache.Cache ehcache = Server.getCacheManager().getCache(cache);
        if (ehcache == null) {
            throw new NotFoundException("Cache not found");
        } else {
            Server.getCacheManager().removeCache(cache);
        }
    }


    /**
     * Routes the reqeust to an {@link ElementResource}
     * @param element
     * @return
     */
    @Path(value = "{element}", limited = false)
    public ElementResource getElementResource(@PathParam("element")String element) {
        return new ElementResource(uriInfo, request, cache, element);
    }

}

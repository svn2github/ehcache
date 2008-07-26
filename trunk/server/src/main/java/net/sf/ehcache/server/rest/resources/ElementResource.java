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
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.server.jaxb.Element;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.HEAD;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class ElementResource {

    private static final Logger LOG = Logger.getLogger(ElementResource.class.getName());

    private static final CacheManager MANAGER;

    static {
        MANAGER = CacheManager.getInstance();
    }

    /**
     * The full URI for the resource
     */
    private UriInfo uriInfo;

    /**
     * The HTTP request
     */
    private Request request;

    /**
     * The cache this resource belongs to
     */
    private String cache;

    /**
     * The element attached to this resource
     */
    private String element;

    /**
     * Full constructor
     * @param uriInfo
     * @param request
     * @param cache
     * @param element
     */
    public ElementResource(UriInfo uriInfo, Request request,
                           String cache, String element) {
        this.uriInfo = uriInfo;
        this.request = request;
        this.cache = cache;
        this.element = element;
    }


    /**
     * HEAD method implementation
     * @return
     * todo consider getting the Element and setting last modified
     */
    @HEAD
    public Response getElementHeader() {
        LOG.log(Level.FINE, "HEAD element {}" + element);

        net.sf.ehcache.Cache ehcache = MANAGER.getCache(this.cache);
        if (ehcache == null) {
            throw new NotFoundException("Cache not found: " + cache);
        }
        boolean exists = ehcache.isKeyInCache(element);
        if (!exists) {
            throw new NotFoundException("Element not found: " + element);
        }
        return Response.ok().build();
    }

    /**
     * Implements the GET method.
     * @return
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.11">HTTP/1.1 section 3.11</a>
     */
    @GET
    public Response getElement() {
        LOG.log(Level.FINE, "GET element {}" + element);
        net.sf.ehcache.Cache ehcache = MANAGER.getCache(cache);
        net.sf.ehcache.Element ehcacheElement = ehcache.get(element);
        if (ehcacheElement == null) {
            throw new NotFoundException("Element not found: " + element);
        }

        //what about if the value is not put in via this RESTful web service?
        //todo check element value
        //todo preserve mimetype rather than hardcoding to application/xml
        Element localElement;
        Object value = ehcacheElement.getObjectValue();
        if (value instanceof Element) {
            localElement = (Element) value;
        } else {
            localElement = new Element((byte[]) ehcacheElement.getObjectValue(), uriInfo.getAbsolutePath().toString(), "application/xml");
            localElement.setMimeType("application/xml");
        }


        long lastModified = ehcacheElement.getLastUpdateTime();
        if (lastModified == 0) {
            //created but not "updated"
            lastModified = ehcacheElement.getCreationTime();
        }
        Date lastModifiedDate = new Date(lastModified);
        //This will be unique across JVM restarts, or deleting an element and putting one back in.
        long eTagNumber = lastModified + ehcacheElement.getVersion();

        //HTTP/1.1 ETag - we just use
        EntityTag entityTag = new EntityTag(new StringBuffer().append(eTagNumber).toString());

        Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(lastModifiedDate, entityTag);
        //return 304?
        if (responseBuilder != null) {
            return responseBuilder.build();
        }

        //return the data?
        return Response.ok(localElement.getValue(), localElement.getMimeType()).lastModified(lastModifiedDate).tag(entityTag).build();
    }

    /**
     * Implements the PUT method
     * @param headers
     * @param data
     * @return
     */
    @PUT
    public Response putElement(@Context HttpHeaders headers, byte[] data) {
        LOG.info("PUT element " + cache + " " + this.element);
        net.sf.ehcache.Cache ehcache = CacheManager.getInstance().getCache(cache);
        if (element == null) {
            throw new NotFoundException("Cache " + cache + " does not exist.");
        }


        URI uri = uriInfo.getAbsolutePath();
        MediaType mimeType = headers.getMediaType();
        Element localElement = new Element(data, uri.toString(), mimeType.toString());

        Response response;

        if (cache != null) {
            response = Response.created(uri).build();
        } else {
            response = Response.noContent().build();
        }

        //todo how to cater for this element metadata
        ehcache.put(new net.sf.ehcache.Element(this.element, localElement));

            // Create the cache if one has not been created
//            URI cacheUri = uriInfo.getAbsolutePathBuilder().path("..").build().normalize();
        return response;
    }

    /**
     * Implements the DELETE method
     */
    @DELETE
    public void deleteElement() {
        LOG.info("DELETE element " + cache + " " + element);

        net.sf.ehcache.Cache ehcache = CacheManager.getInstance().getCache(cache);
        boolean removed = ehcache.remove(element);
        if (!removed) {
            throw new NotFoundException("Element not found");
        }
    }
}

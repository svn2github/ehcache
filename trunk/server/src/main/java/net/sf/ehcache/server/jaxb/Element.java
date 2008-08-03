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

package net.sf.ehcache.server.jaxb;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.MimeTypeByteArray;
import net.sf.ehcache.util.MemoryEfficientByteArrayOutputStream;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;

/**
 * A representation of a core Ehcache Element.
 * <p/>
 * Caches have default settings for timeToLive, timeToIdle and eternal. If any of those is set
 * then defaults are not applied.
 *
 * @author Greg Luck
 * @version $Id$
 */

@XmlRootElement
public class Element {

    /**
     * A representation of the payload value of the Element.
     * Used with MIME Type to figure out what to do with it.
     */
    private byte[] value;

    /**
     * The RESTful resource address for this Element. Only relevant where the
     * server is configured to serve RESTful Web Services.
     */
    private String resourceUri;

    /**
     * When Elements are PUT into the cache, a MIME Type should be set in the request header.
     * The MIME Type is preserved and put into the response header when a GET is done.
     * <p/>
     * Some common MIME Types which are expected to be used by clients are:
     * <p/>
     * <ul>
     * <li><code>text/plain</code> Plain text
     * <li><code>application/xml</code> Extensible Markup Language. Defined in RFC 3023
     * <li><code>application/json</code> JavaScript Object Notation JSON. Defined in RFC 4627
     * <li><code>application/x-java-serialized-object</code> A serialized Java object
     * </ul>
     */
    private String mimeType;


    private Object key;

    private Long version;

    private Long creationTime;
    private Long lastUpdateTime;
    private Long expirationDate;

    private Boolean eternal;
    private Integer timeToIdleSeconds;
    private Integer timeToLiveSeconds;


    /**
     * Empty Constructor
     */
    public Element() {
    }

    /**
     * Full constructor
     *
     * @param value
     * @param resourceUri
     * @param mimeType
     */
    public Element(byte[] value, String resourceUri, String mimeType) {
        setValue(value);
        setResourceUri(resourceUri);
        setMimeType(mimeType);
    }

    /**
     * Constructor which takes an Ehcache core Element.
     * <p/>
     * The {@link #mimeType} and {@link #value} are stored in
     * the core Ehcache <code>value</code> field using {@link net.sf.ehcache.MimeTypeByteArray}
     * <p/>
     * If the MIME Type is not set, an attempt is made to set a sensible value. The rules for setting the Mime Type are:
     * <ol>
     * <li>If the value in element is null, the <code>mimeType</code> is set to null.
     * <li>If we stored the mimeType in ehcache, then <code>mimeType</code> is set with it.
     * <li>If no mimeType was set and the value is a <code>byte[]</code> the <code>mimeType</code> is set to "application/octet-stream".
     * <li>If no mimeType was set and the value is a <code>String</code> the <code>mimeType</code> is set to "text/plain".
     * </ol>
     * @param element the ehcache core Element
     * @throws CacheException if an Exception occurred in the underlying cache.
     */
    public Element(net.sf.ehcache.Element element, String resourceUri) throws CacheException {
        this(element);
        this.resourceUri = resourceUri;
    }

    /**
     * Constructor which takes an Ehcache core Element.
     * <p/>
     * The {@link #mimeType} and {@link #value} are stored in
     * the core Ehcache <code>value</code> field using {@link net.sf.ehcache.MimeTypeByteArray}
     * <p/>
     * If the MIME Type is not set, an attempt is made to set a sensible value. The rules for setting the Mime Type are:
     * <ol>
     * <li>If the value in element is null, the <code>mimeType</code> is set to null.
     * <li>If we stored the mimeType in ehcache, then <code>mimeType</code> is set with it.
     * <li>If no mimeType was set and the value is a <code>byte[]</code> the <code>mimeType</code> is set to "application/octet-stream".
     * <li>If no mimeType was set and the value is a <code>String</code> the <code>mimeType</code> is set to "text/plain".
     * </ol>
     * @param element the ehcache core Element
     * @throws CacheException if an Exception occurred in the underlying cache.
     */
    public Element(net.sf.ehcache.Element element) throws CacheException {

        key = element.getKey();
        expirationDate = element.getExpirationTime();

        Object ehcacheValue = element.getObjectValue();

        if (ehcacheValue == null) {
            this.value = null;
            this.mimeType = null;
        } if (ehcacheValue instanceof MimeTypeByteArray) {
            //we have Mime Type data to extract
            mimeType = ((MimeTypeByteArray) ehcacheValue).getMimeType();
            this.value = ((MimeTypeByteArray) ehcacheValue).getValue();
        } else if (ehcacheValue instanceof byte[]) {
            //already a byte[]
            this.value = (byte[]) ehcacheValue;
            mimeType = "application/octet-stream";
        } else if (ehcacheValue instanceof String) {
            //a String such as XML
            this.value = ((String) ehcacheValue).getBytes();
            this.mimeType = "text/plain";
        } else {
            //A type we do not handle therefore serialize using Java Serialization
            MemoryEfficientByteArrayOutputStream stream = null;
            try {
                stream = MemoryEfficientByteArrayOutputStream.serialize(element.getValue());
            } catch (IOException e) {
                throw new CacheException(e);
            }
            this.value = stream.getBytes();
            this.mimeType = "application/x-java-serialized-object";
        }

    }

    /**
     * The key for this element. It needs to be Serializable. To work with the RESTful API it needs to be a String.
     * @return the value of the key
     */
    public Object getKey() {
        return key;
    }

    /**
     * The key for this element. It needs to be Serializable. To work with the RESTful API it needs to be a String. 
     * @param key the value of the key
     */
    public void setKey(Object key) {
        this.key = key;
    }

    /**
     * Sets the payload
     * Should be set in conjunction with the MIME type
     *
     * @param value the value as bytes
     */
    public void setValue(byte[] value) {
        this.value = value;
    }

    /**
     * Gets the payload.
     * Use the MIME type to work out how to decode this
     *
     * @return the value as bytes
     */
    public byte[] getValue() {
        return value;
    }

    /**
     * Gets the URI for this resource
     *
     * @return a string being a fully qualified URI e.g. http://localhost:8080/ehcache/rest/sampleCache1/34
     */
    public String getResourceUri() {
        return resourceUri;
    }

    /**
     * Sets the URI for this resource
     *
     * @param resourceUri a string being a fully qualified URI e.g. http://localhost:8080/ehcache/rest/sampleCache1/34
     */
    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    /**
     * From ehcache-1.6, ehcache supports non-Java clients. The Web Services serialize the element
     * value as a byte[]. That byte array could be of any type. To give clients assistance in determining
     * the type, a MIME Type has been added.
     * <p/>
     * Gets the MIME Type.
     * <p/>
     * When Elements are put into the cache, a MIME Type should be set.
     * <p/>
     * The MIME Type is preserved and put into the response header when a GET is done.
     * <p/>
     * Some common MIME Types which are expected to be used by clients are:
     * <ul>
     * <li><code>text/plain</code> Plain text
     * <li><code>application/xml</code>Extensible Markup Language. Defined in RFC 3023
     * <li><code>application/json</code>JavaScript Object Notation JSON. Defined in RFC 4627
     * <li><code>application/x-java-serialized-object</code>A serialized Java object
     * </ul>
     * A Mime Type of null indicates the value is a Java Object.
     * @return
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the MIME Type
     *
     * @param mimeType
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Sets time to Live
     *
     * @param timeToLiveSeconds the number of seconds to live
     */
    public void setTimeToLiveSeconds(Integer timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;


    }

    /**
     * Sets time to idle
     *
     * @param timeToIdleSeconds the number of seconds to idle
     */
    public void setTimeToIdle(Integer timeToIdleSeconds) {
        this.timeToIdleSeconds = timeToIdleSeconds;
    }

    /**
     * If there is an Element in the Cache and it is replaced with a new Element for the same key,
     * then both the version number and lastUpdateTime should be updated to reflect that. The creation time
     * will be the creation time of the new Element, not the original one, so that TTL concepts still work.
     *
     * @return the time when the last update occured. If this is the original Element, the time will be null
     */
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }


    /**
     * @return true if the element is eternal
     */
    public Boolean isEternal() {
        return eternal;
    }

    /**
     * Sets whether the element is eternal.
     *
     * @param eternal true if it lives forever
     */
    public void setEternal(Boolean eternal) {
        this.eternal = eternal;
    }

    /**
     * @return the time to live, in seconds
     */
    public Integer getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    /**
     * @return the time to idle, in seconds
     */
    public Integer getTimeToIdleSeconds() {
        return timeToIdleSeconds;
    }

    /**
     * @return the version of this element
     */
    public long getVersion() {
        return version;
    }

    /**
     * @return the time this element was created in the cache
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * 
     * @param timeToIdleSeconds the time an element may be unused before it becomes stale
     */
    public void setTimeToIdleSeconds(Integer timeToIdleSeconds) {
        this.timeToIdleSeconds = timeToIdleSeconds;
    }

    /**
     * Throws a <code>UnsupportedOperationException</code> if called. This is immutatble but
     * a setter must be provided to support the JavaBeans convention.
     * @throws UnsupportedOperationException do not call this method. This is immutable
     */
    public void setCreationTime(Long creationTime) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Creation time is immutable.");
    }

    /**
     * Throws a <code>UnsupportedOperationException</code> if called. This is immutatble but
     * a setter must be provided to support the JavaBeans convention.
     * @throws UnsupportedOperationException do not call this method. This is immutable
     */
    public void setVersion(Long version) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Version is immutable.");
    }

    /**
     * Throws a <code>UnsupportedOperationException</code> if called. This is immutatble but
     * a setter must be provided to support the JavaBeans convention.
     * @throws UnsupportedOperationException do not call this method. This is immutable
     */
    public void setLastUpdateTime(Long lastUpdateTime) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Last Update Time is immutable.");
    }

    /**
     * Gets the core Ehcache element.
     * @return the core Ehcache element. The {@link #mimeType} and {@link #value} are stored in
     * the core Ehcache <code>value</code> field using {@link net.sf.ehcache.MimeTypeByteArray}
     */
    public net.sf.ehcache.Element getEhcacheElement() {
        MimeTypeByteArray mimeTypeByteArray = new MimeTypeByteArray(mimeType, value);
        return new net.sf.ehcache.Element(key, mimeTypeByteArray, eternal, timeToIdleSeconds, timeToLiveSeconds);
    }

    /**
     * Returns the expiration time of the element
     * @return the best estimate of the expiration date of this element from the cache. If the element is eternal
     *  {@link Long#MAX_VALUE} is returned.
     * @see net.sf.ehcache.Element#getExpirationTime()
     */
    public Long getExpirationDate() {
        return expirationDate;
    }

    /**
     * Do not call. This is immutable from the point of view of a client.
     * @throws UnsupportedOperationException do not call this method. This is immutable.
     */
    public void setExpirationDate(Long expirationDate) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Last Update Time is immutable.");
    }
}

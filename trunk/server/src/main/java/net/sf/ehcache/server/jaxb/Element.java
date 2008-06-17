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

import net.sf.ehcache.util.MemoryEfficientByteArrayOutputStream;
import net.sf.ehcache.CacheException;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;

/**
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
     * <li><code>text/xml</code> Extensible Markup Language. Defined in RFC 3023
     * <li><code>application/json</code> JavaScript Object Notation JSON. Defined in RFC 4627
     * <li><code>application/x-java-serialized-object</code> A serialized Java object
     * </ul>
     */
    private String mimeType;
    private Object key;
    private int timeToIdleSeconds;
    private long creationTime;
    private long version;
    private long lastUpdateTime;
    private long expirationTime;
    private boolean eternal;
    private int timeToLive;
    private int timeToIdle;

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
     *
     * @param element the core Element
     * @throws CacheException if an IOException occurs serializing the value to a byte[].q
     */
    public Element(net.sf.ehcache.Element element) throws CacheException {
        key = element.getKey();

        Object object = element.getObjectValue();

        if (object == null) {
            value = null;
        } else if (object instanceof byte[]) {
            //already a byte[]
            value = (byte[]) object;
        } else if (object instanceof String) {
            //a String such as XML
            value = ((String) object).getBytes();
        } else {
            //A type we do not handle therefore serialize using Java Serialization
            MemoryEfficientByteArrayOutputStream stream = null;
            try {
                stream = MemoryEfficientByteArrayOutputStream.serialize(element.getValue());
            } catch (IOException e) {
                throw new CacheException(e);
            }
            value = stream.getBytes();
        }
    }

    /**
     * @return
     */
    public Object getKey() {
        return key;
    }

    /**
     * @param key
     */
    public void setKey(Object key) {
        this.key = key;
    }

    /**
     * Sets the payload
     *
     * @param value
     */
    public void setValue(byte[] value) {
        this.value = value;
    }

    /**
     * Gets the payload
     *
     * @return
     */
    public byte[] getValue() {
        return value;
    }

    /**
     * Gets the URI for this resource
     *
     * @return
     */
    public String getResourceUri() {
        return resourceUri;
    }

    /**
     * Sets the URI for this resource
     *
     * @param resourceUri
     */
    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    /**
     * Gets the MIME Type.
     *
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
    public void setTimeToLive(int timeToLiveSeconds) {


    }

    /**
     * Sets time to idle
     *
     * @param timeToIdleSeconds the number of seconds to idle
     */
    public void setTimeToIdle(int timeToIdleSeconds) {
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
     * An element is expired if the expiration time as given by {@link #getExpirationTime()} is in the past.
     *
     * @return true if the Element is expired, otherwise false. If no lifespan has been set for the Element it is
     *         considered not able to expire.
     * @see #getExpirationTime()
     */
//    public boolean isExpired() {
//        return super.isExpired();    //To change body of overridden methods use File | Settings | File Templates.
//    }

    /**
     * Returns the expiration time based on time to live. If this element also has a time to idle setting, the expiry
     * time will vary depending on whether the element is accessed.
     *
     * @return the time to expiration
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * @return true if the element is eternal
     */
    public boolean isEternal() {
        return eternal;
    }

    /**
     * Sets whether the element is eternal.
     *
     * @param eternal
     */
    public void setEternal(boolean eternal) {
        this.eternal = eternal;
    }

    /**
     * Whether any combination of eternal, TTL or TTI has been set.
     *
     * @return true if set.
     */
//    public boolean isLifespanSet() {
//        return super.isLifespanSet();
//    }

    /**
     * @return the time to live, in seconds
     */
    public int getTimeToLive() {
        return timeToLive;
    }

    /**
     * @return the time to idle, in seconds
     */
    public int getTimeToIdle() {
        return timeToIdle;
    }

    /**
     * @return
     */
    public long getVersion() {
        return version;
    }

    /**
     * @param version
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * @return
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * @param creationTime
     */
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * @return
     */
    public net.sf.ehcache.Element getEhcacheElement() {
        //todo extra properties.
        return new net.sf.ehcache.Element(key, value);
    }
}

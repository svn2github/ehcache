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
     *
     */
    private String mimeType;

    private String key;

    private int timeToIdleSeconds;
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
        key = element.getKey().toString();
        MemoryEfficientByteArrayOutputStream stream = null;
        try {
            stream = MemoryEfficientByteArrayOutputStream.serialize(element.getValue());
        } catch (IOException e) {
            throw new CacheException(e);
        }
        value = stream.getBytes();
    }

    /**
     * Ehcache core can use <code>Object</code> or <code>Serializable</code> for keys.
     * This class uses <code>String</code>s for keys. <code>toString()</code> is used
     * to convert Ehcache core keys to these keys.
     *
     * @return the key as a String
     */
    public String getKey() {
        return key;
    }

    /**
     * Set the key
     *
     * @param key a non-null String
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Sets the payload
     *
     * @param value can be anything in the byte array. Set {@link #setMimeType(String)} to indicate
     *              the type.
     */
    private void setValue(byte[] value) {
        this.value = value;
    }

    /**
     * Gets the value of the element as a byte[]. An types can be supported, including serialized
     * Java classes and XML.
     *
     * @return the value as a byte[]. Use {@link #getMimeType()} to discover the type.
     */
    public byte[] getValue() {
        return value;
    }

    /**
     * Gets the RESTful URI for this resource. Resources can be accessed by REST regardless of whether
     * they were created that way.
     *
     * @return the RESTful resource for this Element.
     */
    public String getResourceUri() {
        return resourceUri;
    }

    /**
     * Sets the URI for this resource
     *
     * @param resourceUri the URI for the resource
     */
    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    /**
     * Gets the MIME Type.
     * <p/>
     * Some examples which may be relevant are:
     * <ul>
     * <li><code>text/plain</code> which indicates plain text.
     * <li><code>text/xml</code> which indicates Extensible Markup Language. Defined in RFC 3023
     * <li><code>application/json</code> which indicates JavaScript Object Notation JSON. Defined in RFC 4627
     * <li><code>application/x-java-serialized-object</code> which indicates serialized Java objects.
     * </ul>
     * @return the MIME Type of the byte[] value
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the MIME Type.
     *
     * @param mimeType the MIME type of the byte[] value
     * @see #setMimeType(String)
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
        this.timeToLive = timeToLiveSeconds;
    }

    /**
     * Sets time to idle, which is the amount of time an element will live if it has not been accessed
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
     * @param eternal true for eternal
     */
    public void setEternal(boolean eternal) {
        this.eternal = eternal;
    }

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
     * todo implement
     *
     * @return
     */
    public net.sf.ehcache.Element getEhcacheElement() {
        return null;
    }
}

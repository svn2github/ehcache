/**
 *  Copyright 2003-2006 Greg Luck
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


package net.sf.ehcache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A Cache Element, consisting of a key, value and attributes.
 * <p/>
 * From ehcache-1.2, Elements can have keys and values that are Serializable or Objects. To preserve backward
 * compatibility, special accessor methods for Object keys and values are provided: {@link #getObjectKey()} and
 * {@link #getObjectValue()}. If placing Objects in ehcache, developers must use the new getObject... methods to
 * avoid CacheExceptions. The get... methods are reserved for Serializable keys and values.
 *
 * @author Greg Luck
 * @version $Id$
 * @noinspection SerializableHasSerializationMethods
 */
public final class Element implements Serializable, Cloneable {
    /**
     * serial version
     * Updated version 1.2 and again for 1.2.1
     */
    private static final long serialVersionUID = 3343087714201120157L;

    private static final Log LOG = LogFactory.getLog(Element.class.getName());

    private static final int ONE_SECOND = 1000;

    /**
     * the cache key.
     */
    private final Object key;

    /**
     * the value.
     */
    private final Object value;

    /**
     * version of the element. System.currentTimeMillis() is used to compute version for updated elements. That
     * way, the actual version of the updated element does not need to be checked.
     */
    private long version;

    /**
     * The creation time.
     */
    private long creationTime;

    /**
     * The last access time.
     */
    private long lastAccessTime;

    /**
     * The next to last access time. Used by the expiry mechanism
     */
    private long nextToLastAccessTime;

    /**
     * The number of times the element was hit.
     */
    private long hitCount;

    /**
     * The amount of time for the element to live, in seconds. 0 indicates unlimited.
     */
    private int timeToLive;

    /**
     * The amount of time for the element to idle, in seconds. 0 indicates unlimited.
     */
    private int timeToIdle;

    /**
     * If there is an Element in the Cache and it is replaced with a new Element for the same key,
     * then both the version number and lastUpdateTime should be updated to reflect that. The creation time
     * will be the creation time of the new Element, not the original one, so that TTL concepts still work.
     */
    private long lastUpdateTime;

    /**
     * Whether the element is eternal, i.e. never expires.
     */
    private boolean eternal;


    /**
     * Whether any combination of eternal, TTL or TTI has been set.
     */
    private boolean lifespanSet;



    /**
     * A full constructor.
     * <p/>
     * Creation time is set to the current time. Last Access Time and Previous To Last Access Time
     * are not set.
     *
     * @since .4
     */
    public Element(Serializable key, Serializable value, long version) {
        this((Object) key, (Object) value, version);

    }

    /**
     * A full constructor.
     * <p/>
     * Creation time is set to the current time. Last Access Time and Previous To Last Access Time
     * are not set.
     *
     * @since 1.2
     */
    public Element(Object key, Object value, long version) {
        this.key = key;
        this.value = value;
        this.version = version;
        creationTime = System.currentTimeMillis();
        hitCount = 0;
    }

    /**
     * Constructor.
     *
     * @param key
     * @param value
     */
    public Element(Serializable key, Serializable value) {
        this((Object) key, (Object) value, 1L);
    }

    /**
     * Constructor.
     *
     * @param key
     * @param value
     * @since 1.2
     */
    public Element(Object key, Object value) {
        this(key, value, 1L);
    }

    /**
     * Gets the key attribute of the Element object.
     *
     * @return The key value. If the key is not Serializable, null is returned and an info log message emitted
     * @see #getObjectKey()
     */
    public final Serializable getKey() {
        Serializable keyAsSerializable;
        try {
            keyAsSerializable = (Serializable) key;
        } catch (Exception e) {
            throw new CacheException("Key " + key + " is not Serializable. Consider using Element#getObjectKey()");
        }
        return keyAsSerializable;
    }

    /**
     * Gets the key attribute of the Element object.
     * <p/>
     * This method is provided for those wishing to use ehcache as a memory only cache
     * and enables retrieval of non-Serializable values from elements.
     *
     * @return The key as an Object. i.e no restriction is placed on it
     * @see #getKey()
     */
    public final Object getObjectKey() {
        return key;
    }

    /**
     * Gets the value attribute of the Element object.
     *
     * @return The value which must be Serializable. If not use {@link #getObjectValue}. If the value is not Serializable, null is returned and an info log message emitted
     * @see #getObjectValue()
     */
    public final Serializable getValue() {
        Serializable valueAsSerializable;
        try {
            valueAsSerializable = (Serializable) value;
        } catch (Exception e) {
            throw new CacheException("Value " + value + " is not Serializable. Consider using Element#getObjectKey()");
        }
        return valueAsSerializable;
    }

    /**
     * Gets the value attribute of the Element object as an Object.
     * <p/>
     * This method is provided for those wishing to use ehcache as a memory only cache
     * and enables retrieval of non-Serializable values from elements.
     *
     * @return The value as an Object.  i.e no restriction is placed on it
     * @see #getValue()
     * @since 1.2
     */
    public final Object getObjectValue() {
        return value;
    }

    /**
     * Equals comparison with another element, based on the key.
     */
    public final boolean equals(Object object) {
        if (object == null) {
            return false;
        }

        Element element = (Element) object;
        if (key == null || element.getObjectKey() == null) {
            return false;
        }

        return key.equals(element.getObjectKey());
    }

    /**
     * Sets time to Live
     *
     * @param timeToLiveSeconds the number of seconds to live
     */
    public void setTimeToLive(int timeToLiveSeconds) {
        this.timeToLive = timeToLiveSeconds;
        lifespanSet = true;
    }

    /**
     * Sets time to idle
     *
     * @param timeToIdleSeconds the number of seconds to idle
     */
    public void setTimeToIdle(int timeToIdleSeconds) {
        this.timeToIdle = timeToIdleSeconds;
        lifespanSet = true;
    }

    /**
     * Gets the hascode, based on the key.
     */
    public final int hashCode() {
        return key.hashCode();
    }

    /**
     * Sets the version attribute of the ElementAttributes object.
     *
     * @param version The new version value
     */
    public final void setVersion(long version) {
        this.version = version;
    }

    /**
     * Gets the creationTime attribute of the ElementAttributes object.
     *
     * @return The creationTime value
     */
    public final long getCreationTime() {
        return creationTime;
    }

    /**
     * Sets the creationTime attribute of the ElementAttributes object.
     */
    public final void setCreateTime() {
        creationTime = System.currentTimeMillis();
    }

    /**
     * Gets the version attribute of the ElementAttributes object.
     *
     * @return The version value
     */
    public final long getVersion() {
        return version;
    }

    /**
     * Gets the last access time.
     * Access means a get. So a newly created {@link Element}
     * will have a last access time equal to its create time.
     */
    public final long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * Gets the next to last access time. This is package protected as it should
     * not be used outside internal Cache housekeeping.
     *
     * @see #getLastAccessTime()
     */
    final long getNextToLastAccessTime() {
        return nextToLastAccessTime;
    }

    /**
     * Gets the hit count on this element.
     */
    public final long getHitCount() {
        return hitCount;
    }

    /**
     * Resets the hit count to 0 and the last access time to 0.
     */
    public final void resetAccessStatistics() {
        lastAccessTime = 0;
        nextToLastAccessTime = 0;
        hitCount = 0;
    }

    /**
     * Sets the last access time to now.
     */
    public final void updateAccessStatistics() {
        nextToLastAccessTime = lastAccessTime;
        lastAccessTime = System.currentTimeMillis();
        hitCount++;
    }

    /**
     * Sets the last access time to now.
     */
    public final void updateUpdateStatistics() {
        lastUpdateTime = System.currentTimeMillis();
        version = lastUpdateTime;
    }


    /**
     * Returns a {@link String} representation of the {@link Element}.
     */
    public final String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("[ key = ").append(key)
                .append(", value=").append(value)
                .append(", version=").append(version)
                .append(", hitCount=").append(hitCount)
                .append(", CreationTime = ").append(this.getCreationTime())
                .append(", LastAccessTime = ").append(this.getLastAccessTime())
                .append(" ]");

        return sb.toString();
    }

    /**
     * Clones an Element. A completely new object is created, with no common references with the
     * existing one.
     * <p/>
     * This method will not work unless the Object is Serializable
     * <p/>
     * Warning: This can be very slow on large object graphs. If you use this method
     * you should write a performance test to verify suitability.
     *
     * @return a new {@link Element}, with exactly the same field values as the one it was cloned from.
     * @throws CloneNotSupportedException
     */
    public final Object clone() throws CloneNotSupportedException {
        //Not used. Just to get code inspectors to shut up
        super.clone();

        Element element = new Element(deepCopy(key), deepCopy(value), version);
        element.creationTime = creationTime;
        element.lastAccessTime = lastAccessTime;
        element.nextToLastAccessTime = nextToLastAccessTime;
        element.hitCount = hitCount;
        return element;
    }

    private Object deepCopy(Object oldValue) {
        Serializable newValue = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try {
            oos = new ObjectOutputStream(bout);
            oos.writeObject(oldValue);
            ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
            ois = new ObjectInputStream(bin);
            newValue = (Serializable) ois.readObject();
        } catch (IOException e) {
            LOG.error("Error cloning Element with key " + key
                    + " during serialization and deserialization of value");
        } catch (ClassNotFoundException e) {
            LOG.error("Error cloning Element with key " + key
                    + " during serialization and deserialization of value");
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
                if (ois != null) {
                    ois.close();
                }
            } catch (Exception e) {
                LOG.error("Error closing Stream");
            }
        }
        return newValue;
    }

    /**
     * The size of this object in serialized form. This is not the same
     * thing as the memory size, which is JVM dependent. Relative values should be meaningful,
     * however.
     * <p/>
     * Warning: This method can be <b>very slow</b> for values which contain large object graphs.
     *
     * @return The serialized size in bytes
     */
    public final long getSerializedSize() {
        if (!isSerializable()) {
            return 0;
        }
        long size = 0;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bout);
            oos.writeObject(this);
            size = bout.size();
            return size;
        } catch (IOException e) {
            LOG.error("Error measuring element size for element with key " + key);
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (Exception e) {
                LOG.error("Error closing ObjectOutputStream");
            }
        }

        return size;
    }

    /**
     * Whether the element may be Serialized.
     * <p/>
     * While Element implements Serializable, it is possible to create non Serializable elements
     * for use in MemoryStores. This method checks that an instance of Element really is Serializable
     * and will not throw a NonSerializableException if Serialized.
     *
     * @return true if the element is Serializable
     * @since 1.2
     */
    public final boolean isSerializable() {
        return key instanceof Serializable && value instanceof Serializable;
    }

    /**
     * Whether the element's key may be Serialized.
     * <p/>
     * While Element implements Serializable, it is possible to create non Serializable elements and/or
     * non Serializable keys for use in MemoryStores.
     * <p/>
     * This method checks that an instance of an Element's key really is Serializable
     * and will not throw a NonSerializableException if Serialized.
     *
     * @return true if the element's key is Serializable
     * @since 1.2
     */
    public final boolean isKeySerializable() {
        return key instanceof Serializable;
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
    public boolean isExpired() {
        if (!lifespanSet) {
            return false;
        }

        long now = System.currentTimeMillis();
        long expirationTime = getExpirationTime();

        return now > expirationTime;
    }

    /**
     * Returns the expiration time based on time to live. If this element also has a time to idle setting, the expiry
     * time will vary depending on whether the element is accessed.
     *
     * @return the time to expiration
     */
    public long getExpirationTime() {

        if (!lifespanSet || eternal || (timeToLive == 0 && timeToIdle == 0)) {
            return Long.MAX_VALUE;
        }

        long expirationTime = 0;
        long ttlExpiry = creationTime + timeToLive * ONE_SECOND;

        long mostRecentTime = Math.max(creationTime, nextToLastAccessTime);
        long ttiExpiry = mostRecentTime + timeToIdle * ONE_SECOND;

        if (timeToLive != 0 && (timeToIdle == 0 || lastAccessTime == 0)) {
            expirationTime = ttlExpiry;
        } else if (timeToLive == 0) {
            expirationTime = ttiExpiry;
        } else {
            expirationTime = Math.min(ttlExpiry, ttiExpiry);
        }

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
        lifespanSet = true;
    }

    /**
     * Whether any combination of eternal, TTL or TTI has been set.
     *
     * @return true if set.
     */
    public boolean isLifespanSet() {
        return lifespanSet;
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
}




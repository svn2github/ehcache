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
 * {@link #getObjectValue()}. If placing Objects in ehcace, developers must use the new getObject... methods to
 * avoid CacheExceptions. The get... methods are reserved for Serializable keys and values.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class Element implements Serializable, Cloneable {
    /**
     * serial version
     * Updated version 1.2
     */
    static final long serialVersionUID = 7832456720941087574L;

    private static final Log LOG = LogFactory.getLog(Element.class.getName());


    /**
     * the cache key
     */
    private final Object key;

    /**
     * the value
     */
    private Object value;

    /**
     * version of the element
     */
    private long version;

    /**
     * The creation time
     */
    private long creationTime;

    /**
     * The last access time
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
     * A full constructor.
     * <p/>
     * Creation time is set to the current time. Last Access Time and Previous To Last Access Time
     * are not set.
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
     * Constructor
     *
     * @param key
     * @param value
     */
    public Element(Serializable key, Serializable value) {
        this((Object)key, (Object)value, 1L);
    }

    /**
     * Constructor
     *
     * @param key
     * @param value
     * @since 1.2
     */
    public Element(Object key, Object value) {
        this(key, value, 1L);
    }

    /**
     * Gets the key attribute of the Element object
     *
     * @return The key value. If the key is not Serializable, null is returned and an info log message emitted
     * @see #getObjectKey()
     */
    public Serializable getKey() {
        Serializable keyAsSerializable = null;
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
    public Object getObjectKey() {
        return key;
    }

    /**
     * Gets the value attribute of the Element object
     *
     * @return The value which must be Serializable. If not use {@link #getObjectValue}. If the value is not Serializable, null is returned and an info log message emitted
     * @see #getObjectValue()
     */
    public Serializable getValue() {
        Serializable valueAsSerializable = null;
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
    public Object getObjectValue() {
        return value;
    }

    /**
     * Equals comparison with another element, based on the key
     */
    public boolean equals(Object object) {
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
     * Gets the hascode, based on the key
     */
    public int hashCode() {
        return key.hashCode();
    }

    /**
     * Sets the version attribute of the ElementAttributes object
     *
     * @param version The new version value
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Gets the creationTime attribute of the ElementAttributes object
     *
     * @return The creationTime value
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Sets the creationTime attribute of the ElementAttributes object
     */
    public void setCreateTime() {
        creationTime = System.currentTimeMillis();
    }

    /**
     * Gets the version attribute of the ElementAttributes object
     *
     * @return The version value
     */
    public long getVersion() {
        return version;
    }

    /**
     * Gets the last access time.
     * Access means a get. So a newly created {@link Element}
     * will have a last access time equal to its create time.
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * Gets the next to last access time. This is package protected as it should
     * not be used outside internal Cache housekeeping
     *
     * @see #getLastAccessTime()
     */
    long getNextToLastAccessTime() {
        return nextToLastAccessTime;
    }

    /**
     * Gets the hit count on this element.
     */
    public long getHitCount() {
        return hitCount;
    }

    /**
     * Resets the hit count to 0 and the last access time to 0
     */
    public void resetAccessStatistics() {
        lastAccessTime = 0;
        nextToLastAccessTime = 0;
        hitCount = 0;
    }

    /**
     * Sets the last access time to now.
     */
    public void updateAccessStatistics() {
        nextToLastAccessTime = lastAccessTime;
        lastAccessTime = System.currentTimeMillis();
        hitCount++;
    }

    /**
     * Returns a {@link String} representation of the {@link Element}
     */
    public String toString() {
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
     * @return a new {@link Element}, with exactly the same field values as the one it was cloned from.
     * @throws CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException {
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
                oos.close();
                ois.close();
            } catch (IOException e) {
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
    public long getSerializedSize() {
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
                oos.close();
            } catch (IOException e) {
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
     * @return true if the element is Serializable
     * @since 1.2
     */
    public boolean isSerializable() {
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
     * @return true if the element's key is Serializable
     * @since 1.2
     */
    public boolean isKeySerializable() {
        return key instanceof Serializable;
}
        }




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

package net.sf.ehcache.constructs.valueobject;

import java.io.Serializable;
import java.util.Map;

/**
 * A key value pair, useful for moving serialized cache contents around.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public final class KeyValuePair implements Serializable, Map.Entry {

    private static final long serialVersionUID = 6113365291074208556L;
    private final Serializable key;
    private Serializable value;

    /**
     * Creates a KeyValuePair
     * @param key
     * @param value
     */
    public KeyValuePair(Serializable key, Serializable value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Gets a key
     */
    public Object getKey() {
        return key;
    }

    /**
     * Gets a value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Replaces the value corresponding to this entry with the specified
     * value (optional operation).  (Writes through to the map.)  The
     * behavior of this call is undefined if the mapping has already been
     * removed from the map (by the iterator's <tt>remove</tt> operation).
     *
     * @param value new value to be stored in this entry.
     * @return old value corresponding to the entry.
     * @throws IllegalArgumentException      if the value is not Serializable
     */
    public Object setValue(Object value) throws IllegalArgumentException {
        if (!(value instanceof Serializable)) {
            throw new IllegalArgumentException("Value is not serializable");
        }
        Object oldValue = this.value;
        this.value = (Serializable) value;
        return oldValue;
    }
}

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

package net.sf.ehcache.store.policies;

import java.util.Map;

/**
 * A concrete Map.Entry
 * @author Greg Luck
 * @version $Id$
 */
public class Entry implements Map.Entry {

    private Object key;
    private Object value;


    /**
     * Creates a new Map.Entry
     * @param key usually a element's key
     * @param value usually an element
     */
    public Entry(Object key, Object value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the key corresponding to this entry.
     *
     * @return the key corresponding to this entry.
     */
    public Object getKey() {
        return key;
    }

    /**
     * Returns the value corresponding to this entry.  If the mapping
     * has been removed from the backing map (by the iterator's
     * <tt>remove</tt> operation), the results of this call are undefined.
     *
     * @return the value corresponding to this entry.
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
     * @throws UnsupportedOperationException this method should not be called because there is not backing map
     */
    public Object setValue(Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This class is meant only to be used for comparisons");
    }

    /**
     * Returns the hash code value for this map entry.  The hash code
     * for this class is: <pre>
     *     (e.getKey()==null   ? 0 : e.getKey().hashCode())
         * </pre>
     * @return the hash code value for this map entry.
     * @see Object#hashCode()
     * @see Object#equals(Object)
     * @see #equals(Object)
     */
    public int hashCode() {
        if (key == null) {
            return 0;
        } else {
            return key.hashCode();
        }
    }

    // Comparison and hashing

    /**
     * Compares the hashcodes
     *
     * @param o object to be compared for equality with this map.
     * @return <tt>true</tt> if the specified object is equal to this map.
     */
    public boolean equals(Object o) {
        if (o == null && key == null) {
            return true;

        }
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        
        return ((Map.Entry) o).getValue().hashCode() == hashCode();

    }



}

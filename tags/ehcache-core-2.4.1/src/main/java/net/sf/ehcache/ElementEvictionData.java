/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

/**
 * Provides pluggable storage and configuration of TTI eviction data.
 * 
 * @author Geert Bevin
 * @version $Id$
 */
public interface ElementEvictionData extends Cloneable {

    /**
     * Sets the element creation time. Note that this is optional to implement and might result in a no-op.
     * 
     * @param creationTime the new element's creation time
     */
    public void setCreationTime(long creationTime);
    
    /**
     * Get the element's creation time.
     * 
     * @return the element's creation time in seconds
     */
    public long getCreationTime();

    /**
     * Gets the last access time.
     * Access means a get. So a newly created {@link Element} will have a last
     * access time equal to its create time.
     * 
     * @return the element's last access time in seconds
     */
    public long getLastAccessTime();

    /**
     * Updates the last access time.
     * 
     * @param time
     *            the new last access time
     * @param element
     *            the element for which the last access time is set
     */
    public void updateLastAccessTime(long time, Element element);

    /**
     * Resets the last access time.
     * 
     * @param element
     *            the element for which the last access time is set
     */
    public void resetLastAccessTime(Element element);

    /**
     * Creates a clone of the eviction data
     * 
     * @return a clone of the eviction data
     * @throws CloneNotSupportedException
     */
    public ElementEvictionData clone() throws CloneNotSupportedException;

    /**
     * Indicates whether the data of this element eviction instance can
     * participate in serialization of the element as a whole.
     * 
     * @return {@code true} when the data can participate in serialization; or
     *         {@code false} otherwise
     */
    public boolean canParticipateInSerialization();
}

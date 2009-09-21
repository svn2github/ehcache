/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import java.io.Serializable;

/**
 * Provides pluggable storage an configuration of TTI eviction data.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public interface ElementEvictionData extends Serializable, Cloneable {

    /**
     * Get the element's creation time.
     * 
     * @return the element's creation time in seconds
     */
    public int getCreationTime();
    
    /**
     * Gets the last access time.
     * Access means a get. So a newly created {@link Element}
     * will have a last access time equal to its create time.
     * 
     * @return the element's last access time in seconds
     */
    public int getLastAccessTime();

    /**
     * Sets the last access time to 0.
     */
    public void resetLastAccessStatistics();

    /**
     * Sets the last access time to now
     */
    public void updateAccessStatistics();
    
    /**
     * Creates a clone of the eviction data
     * 
     * @return a clone of the eviction data
     * @throws CloneNotSupportedException
     */
    public ElementEvictionData clone() throws CloneNotSupportedException;
}




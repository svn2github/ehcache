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

/**
 * Default implementation of the element eviction data storage that just keeps
 * all the data in instance fields in the heap.
 * 
 * @author Geert Bevin
 * @version $Id: DefaultElementEvictionData.java 1219 2009-09-25 05:10:31Z
 *          gbevin $
 */
public class DefaultElementEvictionData implements ElementEvictionData {

    /**
     * The creation time.
     */
    private int creationTime;
    
    /**
     * The last access time.
     */
    private int lastAccessTime;

    /**
     * Default constructor initializing the field to their empty values
     */
    public DefaultElementEvictionData(int creationTime) {
        this.creationTime = creationTime;
    }
    
    /**
     * Constructor allowing custom values for the data fields.
     * 
     * @param lastAccessTime
     */
    public DefaultElementEvictionData(int creationTime, int lastAccessTime) {
        this.creationTime = creationTime;
        this.lastAccessTime = lastAccessTime;
    }

    /**
     * {@inheritDoc}
     */
    public void setCreationTime(int creationTime) {
        this.creationTime = creationTime;        
    }

    /**
     * {@inheritDoc}
     */
    public int getCreationTime() {
        return creationTime;        
    }

    /**
     * {@inheritDoc}
     */
    public int getLastAccessTime() {
        return lastAccessTime;        
    }

    /**
     * {@inheritDoc}
     */
    public void updateLastAccessTime(int time, Element element) {
        lastAccessTime = time;
    }

    /**
     * {@inheritDoc}
     */
    public void resetLastAccessTime(Element element) {
        lastAccessTime = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ElementEvictionData clone() throws CloneNotSupportedException {
        DefaultElementEvictionData result = (DefaultElementEvictionData)super.clone();
        result.creationTime = this.creationTime;
        result.lastAccessTime = this.lastAccessTime;
        return result;
    }

     /**
      * {@inheritDoc}
      */
    public boolean canParticipateInSerialization() {
        return true;
    }
}
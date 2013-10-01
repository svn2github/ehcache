/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache.statistics.beans;

/**
 * A proxy for Boolean flavored bean proxies.
 *
 * @author cschanck
 */
public class BooleanBeanProxy extends AttributeProxy<Boolean> {

    private Boolean b = Boolean.FALSE;

    /**
     * Instantiates a new boolean bean proxy.
     *
     * @param name the name
     * @param description the description
     * @param isRead the is read
     * @param isWrite the is write
     */
    public BooleanBeanProxy(String name, String description, boolean isRead, boolean isWrite) {
        super(Boolean.class, name, description, isRead, isWrite);
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.beans.AttributeProxy#get(java.lang.String)
     */
    @Override
    public Boolean get(String name) {
        return b;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.beans.AttributeProxy#set(java.lang.String, java.lang.Object)
     */
    @Override
    public void set(String name, Boolean t) {
        b = t;
    }

}

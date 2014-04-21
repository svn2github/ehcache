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
 * An impl of a String attribute proxy.
 *
 * @author cschanck
 */
public class StringBeanProxy extends AttributeProxy<String> {

    private String val = "";

    /**
     * Instantiates a new string bean proxy.
     *
     * @param name the name
     * @param descr the descr
     * @param isRead the is read
     * @param isWrite the is write
     */
    public StringBeanProxy(String name, String descr, boolean isRead, boolean isWrite) {
        super(String.class, name, descr, isRead, isWrite);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.beans.AttributeProxy#get(java.lang.String)
     */
    @Override
    public String get(String name) {
        return val;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statistics.beans.AttributeProxy#set(java.lang.String, java.lang.Object)
     */
    @Override
    public void set(String name, String t) {
        val = t;
    }
}

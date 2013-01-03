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
 * A Class for Double proxies.
 *
 * @author cschanck
 */
public class DoubleBeanProxy extends AttributeProxy<Double> {

    /**
     * Instantiates a new double bean proxy.
     *
     * @param name the name
     * @param description the description
     * @param isRead the is read
     * @param isWrite the is write
     */
    public DoubleBeanProxy(String name, String description, boolean isRead, boolean isWrite) {
        super(Double.class, name, description, isRead, isWrite);
    }

}

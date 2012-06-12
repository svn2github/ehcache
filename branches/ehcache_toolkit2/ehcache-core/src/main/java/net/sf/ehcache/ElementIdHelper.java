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

package net.sf.ehcache;

/**
 * Provide access to the package private methods for getting/setting Element id. The id field of an Element is for internal ehcache use only
 * and this class is meant to discourage casual use of the methods from application code
 *
 * @author teck
 */
public class ElementIdHelper {

    /**
     * Is element id set?
     *
     * @param e element to inspect
     * @return true if this element has an id set
     */
    public static boolean hasId(Element e) {
        return e.hasId();
    }

    /**
     * Get the element id
     *
     * @param e element to inspect
     * @return element id
     */
    public static long getId(Element e) {
        return e.getId();
    }

    /**
     * Set the element id
     *
     * @param e element to adjust
     * @param id
     */
    public static void setId(Element e, long id) {
        e.setId(id);
    }

}

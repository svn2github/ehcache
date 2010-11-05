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
package net.sf.ehcache.store;

import net.sf.ehcache.Element;

/**
 * @author Ludovic Orban
 */
public class DefaultElementValueComparator implements ElementValueComparator {

    /**
     * {@inheritDoc}
     */
    public boolean equals(Element e1, Element e2) {
        if (e1.equals(e2)) {
            if (e1.getObjectValue() == null) {
                return e2.getObjectValue() == null;
            } else {
                return e1.getObjectValue().equals(e2.getObjectValue());
            }
        } else {
            return false;
        }
    }

}

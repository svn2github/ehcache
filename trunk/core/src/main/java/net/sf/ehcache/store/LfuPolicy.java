/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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
 * Contains common LFU policy code for use between the LfuMemoryStore and the DiskStore, which also
 * uses an LfuPolicy for evictions.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class LfuPolicy extends Policy {

    /**
     * Finds the least hit of the sampled elements provided
     * @param sampledElements this should be a random subset of the population
     * @param justAdded we never want to select the element just added. May be null.
     * @return the least hit
     */
    public Element selectedBasedOnPolicy(Element[] sampledElements, Element justAdded) {
        //edge condition when Memory Store configured to size 0
        if (sampledElements.length == 1 && justAdded != null) {
            return justAdded;
        }
        Element lowestElement = null;
        for (Element element : sampledElements) {
            if (lowestElement == null) {
                if (!element.equals(justAdded)) {
                    lowestElement = element;
                }
            } else {
                if (element.getHitCount() < lowestElement.getHitCount() && !element.equals(justAdded)) {
                    lowestElement = element;
                }
            }
        }
        return lowestElement;
    }

}

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

package net.sf.ehcache.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
/**
 *
 * An utility class to merge several enumerations into a single one
 *
 * @author Anthony Dahanne
 *
 * @param <E>
 */
public class MergedEnumeration<E> implements Enumeration<E> {

    private final Enumeration<E> enumeration;

    /**
     * Merges all enumerations found as constructor arguments into a single one
     *
     * @param enumerations
     */
    public MergedEnumeration(Enumeration<E>... enumerations) {
        List<E>  list = new ArrayList<E>();
        for (Enumeration<E> element : enumerations) {
            while (element.hasMoreElements()) {
                E e = element.nextElement();
                list.add(e);
            }
        }
        enumeration = Collections.enumeration(list);
    }

    @Override
    public boolean hasMoreElements() {
        return enumeration.hasMoreElements();
    }

    @Override
    public E nextElement() {
        return enumeration.nextElement();
    }

}

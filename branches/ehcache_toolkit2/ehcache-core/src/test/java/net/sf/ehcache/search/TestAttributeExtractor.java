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

package net.sf.ehcache.search;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.Person;
import net.sf.ehcache.search.attribute.AttributeExtractor;

public class TestAttributeExtractor implements AttributeExtractor {

    public Object attributeFor(Element element, String attributeName) {
        if (! attributeName.equals("age")) {
            throw new AssertionError(attributeName);
        }

        Person person = (Person) element.getObjectValue();
        return person.getAge();
    }

}

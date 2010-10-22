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

import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeType;

/**
 * Accessor for search attributes for a particular cache element
 * 
 * @author teck
 */
public interface ElementAttributeValues {

    /**
     * Get the value for the given attribute and verify the type
     * 
     * @param attributeName name of the attribute to fetch
     * @param expectedType expected type for the attribute
     * @throws SearchException if the attribute is not defined for this cache or there is a type mismatch
     * @return the attribute value
     */
    Object getAttributeValue(String attributeName, AttributeType expectedType) throws SearchException;
    
    /**
     * Get the value for the given attribute
     * 
     * @param attributeName name of the attribute to fetch
     * @throws SearchException if the attribute is not defined for this cache
     * @return the attribute value
     */
    Object getAttributeValue(String attributeName) throws SearchException; 
    
}

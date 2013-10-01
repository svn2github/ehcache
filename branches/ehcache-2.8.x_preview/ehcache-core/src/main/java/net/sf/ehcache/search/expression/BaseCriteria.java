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

package net.sf.ehcache.search.expression;

import java.util.Map;

import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;

/**
 * Base class for all criteria types
 *
 * @author teck
 */
public abstract class BaseCriteria implements Criteria {

    /**
     * {@inheritDoc}
     */
    public Criteria and(Criteria other) {
        return new And(this, other);
    }

    /**
     * {@inheritDoc}
     */
    public Criteria not() {
        return new Not(this);
    }

    /**
     * {@inheritDoc}
     */
    public Criteria or(Criteria other) {
        return new Or(this, other);
    }

    /**
     * For given attribute name, return its corresponding extractors from supplied map, if it exists. Otherwise, throw an exception.
     * @param attrName
     * @param knownExtractors
     * @return
     * @throws SearchException
     */
    public static AttributeExtractor getExtractor(String attrName,
            Map<String, AttributeExtractor> knownExtractors) throws SearchException
    {
        AttributeExtractor extr = knownExtractors.get(attrName);
        if (extr != null) {
            return extr;
        }
        throw new SearchException("Unknown search attribute " + attrName);
    }

}

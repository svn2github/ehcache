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

package net.sf.ehcache.search.expression;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementAttributeValues;

/**
 * A search criteria composed of the logical "and" of two or more other criteria
 *
 * @author teck
 */
public class And extends BaseCriteria {

    private final Criteria[] criterion;

    /**
     * Simple constructor for two criteria
     *
     * @param lhs the left hand side of the "and" expression
     * @param rhs the right hand side of the "and" expression
     */
    public And(Criteria lhs, Criteria rhs) {
        this.criterion = new Criteria[]{lhs, rhs};
    }

    /**
     * Return criterion
     *
     * @return criterion
     */
    public Criteria[] getCriterion() {
        return this.criterion;
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(Element e, ElementAttributeValues attributeValues) {
        for (Criteria c : criterion) {
            if (!c.execute(e, attributeValues)) {
                return false;
            }
        }

        return true;
    }
}

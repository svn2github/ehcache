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

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;

/**
 * A search criteria composed of the logical "or" of two or more other criteria
 *
 * @author teck
 */
public class Or extends BaseCriteria {

    private final Criteria[] criteria;

    /**
     * Simple constructor for two criteria
     *
     * @param lhs the left hand side of the "or" expression
     * @param rhs the right hand side of the "or" expression
     */
    public Or(Criteria lhs, Criteria rhs) {
        this.criteria = new Criteria[] {lhs, rhs};
    }

    private Or(Or original, Criteria additional) {
        Criteria[] originalCriteria = original.getCriterion();
        this.criteria = new Criteria[originalCriteria.length + 1];
        System.arraycopy(originalCriteria, 0, criteria, 0, originalCriteria.length);
        this.criteria[originalCriteria.length] = additional;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Criteria or(Criteria other) {
        return new Or(this, other);
    }

    /**
     * Return criteria
     *
     * @return criteria
     */
    public Criteria[] getCriterion() {
        return criteria;
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(Element e, Map<String, AttributeExtractor> attributeExtractors) {
        for (Criteria c : criteria) {
            if (c.execute(e, attributeExtractors)) {
                return true;
            }
        }
        return false;
    }
}

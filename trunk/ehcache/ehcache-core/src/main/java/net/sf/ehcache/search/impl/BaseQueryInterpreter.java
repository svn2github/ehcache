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

package net.sf.ehcache.search.impl;

import java.util.List;
import java.util.Set;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.aggregator.AggregatorInstance;
import net.sf.ehcache.search.aggregator.Average;
import net.sf.ehcache.search.aggregator.Count;
import net.sf.ehcache.search.aggregator.Max;
import net.sf.ehcache.search.aggregator.Min;
import net.sf.ehcache.search.aggregator.Sum;
import net.sf.ehcache.search.expression.AlwaysMatch;
import net.sf.ehcache.search.expression.And;
import net.sf.ehcache.search.expression.Between;
import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.search.expression.EqualTo;
import net.sf.ehcache.search.expression.GreaterThan;
import net.sf.ehcache.search.expression.GreaterThanOrEqual;
import net.sf.ehcache.search.expression.ILike;
import net.sf.ehcache.search.expression.InCollection;
import net.sf.ehcache.search.expression.LessThan;
import net.sf.ehcache.search.expression.LessThanOrEqual;
import net.sf.ehcache.search.expression.Not;
import net.sf.ehcache.search.expression.NotEqualTo;
import net.sf.ehcache.search.expression.Or;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.StoreQuery.Ordering;

/**
 * Base class for interpreting store queries.
 *
 * @author teck
 */
public abstract class BaseQueryInterpreter {

    /**
     * Interpret this query
     *
     * @param query
     */
    public void process(StoreQuery query) {
        includeKeys(shouldIncludeKeys(query));
        includeValues(shouldIncludeValues(query));
        maxResults(query.maxResults());
        processCriteria(query.getCriteria());
        processAttributes(query.requestedAttributes());
        processOrdering(query.getOrdering());
        processGroupBy(query.groupByAttributes());
        processAggregators(query.getAggregatorInstances());
    }

    /**
     * subclass hook to alter key inclusion behavior
     *
     * @param query
     * @return
     */
    protected boolean shouldIncludeValues(StoreQuery query) {
        return query.requestsValues();
    }

    /**
     * subclass hook to alter value inclusion behavior
     *
     * @param query
     * @return
     */
    protected boolean shouldIncludeKeys(StoreQuery query) {
        return query.requestsKeys();
    }

    private void processAggregators(List<AggregatorInstance<?>> aggregatorInstances) {
        for (AggregatorInstance aggregatorInstance : aggregatorInstances) {
            if (aggregatorInstance instanceof Count) {
                count();
            } else if (aggregatorInstance instanceof Average) {
                average(aggregatorInstance.getAttribute().getAttributeName());
            } else if (aggregatorInstance instanceof Sum) {
                sum(aggregatorInstance.getAttribute().getAttributeName());
            } else if (aggregatorInstance instanceof Min) {
                min(aggregatorInstance.getAttribute().getAttributeName());
            } else if (aggregatorInstance instanceof Max) {
                max(aggregatorInstance.getAttribute().getAttributeName());
            } else {
                throw new SearchException("unknown aggregator type: " + aggregatorInstance.getClass().getName());
            }
        }
    }

    private void processAttributes(Set<Attribute<?>> attributes) {
        for (Attribute<?> attr : attributes) {
            attribute(attr.getAttributeName());
        }
    }

    private void processOrdering(List<Ordering> orderings) {
        for (Ordering ordering : orderings) {
            String attributeName = ordering.getAttribute().getAttributeName();
            if (Direction.DESCENDING.equals(ordering.getDirection())) {
                attributeDescending(attributeName);
            } else {
                attributeAscending(attributeName);
            }
        }
    }

    private void processGroupBy(Set<Attribute<?>> attributes) {
        for (Attribute attr : attributes) {
            groupBy(attr.getAttributeName());
        }
    }

    private void processCriteria(Criteria criteria) {
        if (criteria instanceof AlwaysMatch) {
            processAlwaysCriteria(AlwaysMatch.class.cast(criteria));
        } else if (criteria instanceof And) {
            processAndCriteria(And.class.cast(criteria));
        } else if (criteria instanceof Or) {
            processOrCriteria(Or.class.cast(criteria));
        } else if (criteria instanceof Not) {
            processNotCriteria(Not.class.cast(criteria));
        } else if (criteria instanceof NotEqualTo) {
            processNotEqualCriteria(NotEqualTo.class.cast(criteria));
        } else if (criteria instanceof Between) {
            processBetweenCriteria(Between.class.cast(criteria));
        } else if (criteria instanceof EqualTo) {
            processEqualCriteria(EqualTo.class.cast(criteria));
        } else if (criteria instanceof ILike) {
            processLikeCriteria(ILike.class.cast(criteria));
        } else if (criteria instanceof GreaterThan) {
            processGreaterThanCriteria(GreaterThan.class.cast(criteria));
        } else if (criteria instanceof GreaterThanOrEqual) {
            processGreaterThanOrEqualCriteria(GreaterThanOrEqual.class.cast(criteria));
        } else if (criteria instanceof InCollection) {
            processInCollectionCriteria(InCollection.class.cast(criteria));
        } else if (criteria instanceof LessThan) {
            processLessThanCriteria(LessThan.class.cast(criteria));
        } else if (criteria instanceof LessThanOrEqual) {
            processLessThanOrEqualCriteria(LessThanOrEqual.class.cast(criteria));
        } else {
            throw new SearchException("Unknown criteria type: " + criteria);
        }
    }

    private void processLikeCriteria(ILike criteria) {
        ilike(criteria.getAttributeName(), criteria.getRegex());
    }

    private void processAlwaysCriteria(AlwaysMatch cast) {
        all();
    }

    private void processAndCriteria(And criteria) {
        beginGroup();
        and();
        for (Criteria element : criteria.getCriterion()) {
            processCriteria(element);
        }
        endGroup();
    }

    private void processOrCriteria(Or criteria) {
        beginGroup();
        or();
        for (Criteria element : criteria.getCriterion()) {
            processCriteria(element);
        }
        endGroup();
    }

    private void processInCollectionCriteria(InCollection criteria) {
        beginGroup();
        or();
        for (Object value : criteria.values()) {
            term(criteria.getAttributeName(), value);
        }
        endGroup();
    }

    private void processNotCriteria(Not not) {
        Criteria negated = not.getCriteria();

        if (negated instanceof ILike) {
            ILike ilike = (ILike) negated;
            notIlike(ilike.getAttributeName(), ilike.getRegex());
        } else {
            processCriteria(notOf(negated));
        }
    }

    private static Criteria notOf(Criteria c) {
        if (c instanceof NotEqualTo) {
            return new EqualTo(((NotEqualTo) c).getAttributeName(), ((NotEqualTo) c).getValue());
        } else if (c instanceof EqualTo) {
            return new NotEqualTo(((EqualTo) c).getAttributeName(), ((EqualTo) c).getValue());
        } else if (c instanceof And) {
            Criteria[] criterion = ((And) c).getCriterion();
            Criteria rv = new Or(notOf(criterion[0]), notOf(criterion[1]));
            for (int i = 2; i < criterion.length; i++) {
                rv = rv.or(notOf(criterion[i]));
            }
            return rv;
        } else if (c instanceof Or) {
            Criteria[] criterion = ((Or) c).getCriterion();
            Criteria rv = new And(notOf(criterion[0]), notOf(criterion[1]));
            for (int i = 2; i < criterion.length; i++) {
                rv = rv.and(notOf(criterion[i]));
            }
            return rv;
        } else if (c instanceof Between) {
            Between b = (Between) c;
            String name = b.getAttributeName();
            Criteria lhs = b.isMinInclusive() ? new LessThan(name, b.getMin()) : new LessThanOrEqual(name, b.getMin());
            Criteria rhs = b.isMaxInclusive() ? new GreaterThan(name, b.getMax()) : new GreaterThanOrEqual(name, b.getMax());
            return new Or(lhs, rhs);
        } else if (c instanceof GreaterThan) {
            return new LessThanOrEqual(((GreaterThan) c).getAttributeName(), ((GreaterThan) c).getComparableValue());
        } else if (c instanceof GreaterThanOrEqual) {
            return new LessThan(((GreaterThanOrEqual) c).getAttributeName(), ((GreaterThanOrEqual) c).getComparableValue());
        } else if (c instanceof LessThan) {
            return new GreaterThanOrEqual(((LessThan) c).getAttributeName(), ((LessThan) c).getComparableValue());
        } else if (c instanceof LessThanOrEqual) {
            return new GreaterThan(((LessThanOrEqual) c).getAttributeName(), ((LessThanOrEqual) c).getComparableValue());
        } else if (c instanceof Not) {
            return ((Not) c).getCriteria();
        } else if (c instanceof InCollection) {
            InCollection in = (InCollection) c;
            String name = in.getAttributeName();
            Object[] values = in.values().toArray();
            if (values.length == 1) {
                return new NotEqualTo(in.getAttributeName(), values[0]);
            }

            Criteria rv = new And(new NotEqualTo(name, values[0]), new NotEqualTo(name, values[1]));
            for (int i = 2; i < values.length; i++) {
                rv = rv.and(new NotEqualTo(name, values[i]));
            }
            return rv;
        } else if (c instanceof AlwaysMatch) {
            throw new UnsupportedOperationException();
        } else {
            throw new AssertionError("negate for " + c.getClass());
        }
    }

    private void processNotEqualCriteria(NotEqualTo criteria) {
        notEqualTerm(criteria.getAttributeName(), criteria.getValue());
    }

    private void processBetweenCriteria(Between criteria) {
        between(criteria.getAttributeName(), criteria.getMin(), criteria.getAttributeName(), criteria.getMax(), criteria.isMinInclusive(),
                criteria.isMaxInclusive());
    }

    private void processEqualCriteria(EqualTo criteria) {
        term(criteria.getAttributeName(), criteria.getValue());
    }

    private void processGreaterThanCriteria(GreaterThan criteria) {
        greaterThan(criteria.getAttributeName(), criteria.getComparableValue());
    }

    private void processGreaterThanOrEqualCriteria(GreaterThanOrEqual criteria) {
        greaterThanEqual(criteria.getAttributeName(), criteria.getComparableValue());
    }

    private void processLessThanCriteria(LessThan criteria) {
        lessThan(criteria.getAttributeName(), criteria.getComparableValue());
    }

    private void processLessThanOrEqualCriteria(LessThanOrEqual criteria) {
        lessThanEqual(criteria.getAttributeName(), criteria.getComparableValue());
    }

    /**
     * hook
     *
     * @param maxResults
     */
    protected abstract void maxResults(int maxResults);

    /**
     * hook
     *
     * @param include
     */
    protected abstract void includeKeys(boolean include);

    /**
     * hook
     *
     * @param include
     */
    protected abstract void includeValues(boolean include);

    /**
     * hook
     *
     * @param name
     */
    protected abstract void max(String name);

    /**
     * hook
     *
     * @param name
     */
    protected abstract void min(String name);

    /**
     * hook
     *
     * @param name
     */
    protected abstract void sum(String name);

    /**
     * hook
     *
     * @param name
     */
    protected abstract void average(String name);

    /**
     * hook
     */
    protected abstract void count();

    /**
     * hook
     *
     * @param name
     */
    protected abstract void attribute(String name);

    /**
     * hook
     *
     * @param name
     */
    protected abstract void attributeAscending(String name);

    /**
     * hook
     *
     * @param name
     */
    protected abstract void attributeDescending(String name);

    /**
     * hook
     *
     * @param name
     */
    protected abstract void groupBy(String name);

    /**
     * hook
     *
     * @param name
     * @param regex
     */
    protected abstract void ilike(String name, String regex);

    /**
     * hook
     */
    protected abstract void all();

    /**
     * hook
     */
    protected abstract void endGroup();

    /**
     * hook
     */
    protected abstract void and();

    /**
     * hook
     */
    protected abstract void or();

    /**
     * hook
     */
    protected abstract void beginGroup();

    /**
     * hook
     *
     * @param name
     * @param value
     */
    protected abstract void term(String name, Object value);

    /**
     * hook
     *
     * @param name
     * @param regex
     */
    protected abstract void notIlike(String name, String regex);

    /**
     * hook
     *
     * @param name
     * @param value
     */
    protected abstract void greaterThan(String name, Object value);

    /**
     * hook
     *
     * @param name
     * @param value
     */
    protected abstract void greaterThanEqual(String name, Object value);

    /**
     * hook
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @param minInclusive
     * @param maxInclusive
     */
    protected abstract void between(String name1, Object value1, String name2, Object value2, boolean minInclusive, boolean maxInclusive);

    /**
     * hook
     *
     * @param name
     * @param value
     */
    protected abstract void notEqualTerm(String name, Object value);

    /**
     * hook
     *
     * @param name
     * @param value
     */
    protected abstract void lessThanEqual(String name, Object value);

    /**
     * hook
     *
     * @param name
     * @param value
     */
    protected abstract void lessThan(String name, Object value);

}

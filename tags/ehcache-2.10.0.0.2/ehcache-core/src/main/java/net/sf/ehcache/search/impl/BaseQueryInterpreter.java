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

package net.sf.ehcache.search.impl;

import java.util.List;
import java.util.Set;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;
import net.sf.ehcache.search.ExecutionHints;
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
import net.sf.ehcache.search.expression.IsNull;
import net.sf.ehcache.search.expression.LessThan;
import net.sf.ehcache.search.expression.LessThanOrEqual;
import net.sf.ehcache.search.expression.Not;
import net.sf.ehcache.search.expression.NotEqualTo;
import net.sf.ehcache.search.expression.NotILike;
import net.sf.ehcache.search.expression.NotNull;
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
        includeKeys(query.requestsKeys());
        includeValues(query.requestsValues());
        maxResults(query.maxResults());
        processCriteria(query.getCriteria());
        processAttributes(query.requestedAttributes());
        processOrdering(query.getOrdering());
        processGroupBy(query.groupByAttributes());
        processAggregators(query.getAggregatorInstances());
        processHints(query.getExecutionHints());
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

    /**
     * @param criteria search criteria
     */
    protected void processCriteria(Criteria criteria) {
        if (criteria instanceof AlwaysMatch) {
            all();
        } else if (criteria instanceof And) {
            and(And.class.cast(criteria));
        } else if (criteria instanceof Or) {
            or(Or.class.cast(criteria));
        } else if (criteria instanceof Not) {
            processNotCriteria(Not.class.cast(criteria));
        } else if (criteria instanceof NotEqualTo) {
            notEqualTerm(NotEqualTo.class.cast(criteria));
        } else if (criteria instanceof NotILike) {
            notIlike(NotILike.class.cast(criteria));
        } else if (criteria instanceof NotNull) {
            notNull(NotNull.class.cast(criteria));
        } else if (criteria instanceof Between) {
            between(Between.class.cast(criteria));
        } else if (criteria instanceof EqualTo) {
            equalTo(EqualTo.class.cast(criteria));
        } else if (criteria instanceof IsNull) {
            isNull(IsNull.class.cast(criteria));
        } else if (criteria instanceof ILike) {
            ilike(ILike.class.cast(criteria));
        } else if (criteria instanceof GreaterThan) {
            greaterThan(GreaterThan.class.cast(criteria));
        } else if (criteria instanceof GreaterThanOrEqual) {
            greaterThanEqual(GreaterThanOrEqual.class.cast(criteria));
        } else if (criteria instanceof InCollection) {
            in(InCollection.class.cast(criteria));
        } else if (criteria instanceof LessThan) {
            lessThan(LessThan.class.cast(criteria));
        } else if (criteria instanceof LessThanOrEqual) {
            lessThanEqual(LessThanOrEqual.class.cast(criteria));
        } else {
            throw new SearchException("Unknown criteria type: " + criteria);
        }
    }

    private void processNotCriteria(Not not) {
        Criteria negated = not.getCriteria();

        processCriteria(notOf(negated));
    }
    
    private void processHints(ExecutionHints hints) {
        if (hints != null) {
            setHints(hints);
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
        } else if (c instanceof ILike) {
            ILike ilike = (ILike) c;
            return new NotILike(ilike.getAttributeName(), ilike.getRegex());
        } else if (c instanceof NotILike) {
            NotILike ni = (NotILike)c;
            return new ILike(ni.getAttributeName(), ni.getRegex());
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
        } else if (c instanceof IsNull) {
            return new NotNull(((IsNull) c).getAttributeName());
        } else if (c instanceof NotNull) {
            return new IsNull(((NotNull) c).getAttributeName());
        } else if (c instanceof AlwaysMatch) {
            throw new UnsupportedOperationException();
        } else {
            throw new AssertionError("negate for " + c.getClass());
        }
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
     */
    protected abstract void ilike(ILike criteria);
    
    /**
     * hook
     *
     */
    protected abstract void isNull(IsNull criteria);
    
    /**
     * hook
     *
     */
    protected abstract void notNull(NotNull criteria);

    /**
     * hook
     */
    protected abstract void all();

    /**
     * hook
     */
    protected abstract void and(And criteria);

    /**
     * hook
     */
    protected abstract void or(Or criteria);

    /**
     * hook
     */
    protected abstract void in(InCollection criteria);

    /**
     * hook
     *
     */
    protected abstract void equalTo(EqualTo criteria);

    /**
     * hook
     *
     */
    protected abstract void notIlike(NotILike criteria);

    /**
     * hook
     *
     */
    protected abstract void greaterThan(GreaterThan criteria);

    /**
     * hook
     *
     */
    protected abstract void greaterThanEqual(GreaterThanOrEqual criteria);

    /**
     * hook
     */
    protected abstract void between(Between criteria);

    /**
     * hook
     */
    protected abstract void notEqualTerm(NotEqualTo term);

    /**
     * hook
     */
    protected abstract void lessThanEqual(LessThanOrEqual lte);

    /**
     * hook
     */
    protected abstract void lessThan(LessThan lt);

    /**
     * hook
     */
    protected abstract void setHints(ExecutionHints hints);
}

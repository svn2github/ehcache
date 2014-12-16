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
package net.sf.ehcache.search.parser;

import net.sf.ehcache.search.aggregator.Aggregator;

public class MAggregate implements ModelElement<Aggregator> {

    /**
     * The aggregation type enum.
     */
    public static enum AggOp {
        /**
         * The Sum.
         */
        Sum,
        /**
         * The Min.
         */
        Min,
        /**
         * The Max.
         */
        Max,
        /**
         * The Average.
         */
        Average,
        /**
         * The Count.
         */
        Count
    }

    ;

    /**
     * The op.
     */
    private final AggOp op;

    /**
     * The attribute.
     */
    private final MAttribute ma;

    /**
     * Instantiates a new m aggregate.
     *
     * @param op the operation
     * @param ma the attribute
     */
    public MAggregate(AggOp op, MAttribute ma) {
        this.op = op;
        this.ma = ma;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return op.toString().toLowerCase() + "(" + ma + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ma == null) ? 0 : ma.hashCode());
        result = prime * result + ((op == null) ? 0 : op.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MAggregate other = (MAggregate)obj;
        if (ma == null) {
            if (other.ma != null) return false;
        } else if (!ma.equals(other.ma)) return false;
        if (op != other.op) return false;
        return true;
    }

    public AggOp getOp() {
        return op;
    }

    public MAttribute getAttribute() {
        return ma;
    }

    /**
     * Return this model aggregator as an ehacache aggregator.
     *
     * @return the aggregator
     */
    public Aggregator asEhcacheObject(ClassLoader loader) {
        switch (op) {
            case Sum:
                return ma.asEhcacheObject(loader).sum();
            case Min:
                return ma.asEhcacheObject(loader).min();
            case Max:
                return ma.asEhcacheObject(loader).max();
            case Count:
                return ma.asEhcacheObject(loader).count();
            case Average:
                return ma.asEhcacheObject(loader).average();
        }
        throw new IllegalStateException("Unknown agg operator: " + op);
    }

}

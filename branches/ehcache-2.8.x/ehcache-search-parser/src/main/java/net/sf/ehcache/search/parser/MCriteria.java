/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package net.sf.ehcache.search.parser;

import java.util.Arrays;

import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.search.expression.EqualTo;
import net.sf.ehcache.search.expression.GreaterThan;
import net.sf.ehcache.search.expression.GreaterThanOrEqual;
import net.sf.ehcache.search.expression.LessThan;
import net.sf.ehcache.search.expression.LessThanOrEqual;
import net.sf.ehcache.search.expression.NotEqualTo;


/**
 * Parser model criteria.
 */
public interface MCriteria extends ModelElement<Criteria> {

    /**
     * The Enum for simple operations.
     */
    public enum SimpleOp {

        /**
         * The ge.
         */
        GE(">="),
        /**
         * The lt.
         */
        LT("<"),
        /**
         * The gt.
         */
        GT(">"),
        /**
         * The le.
         */
        LE("<"),
        /**
         * The eq.
         */
        EQ("="),
        /**
         * The ne.
         */
        NE("!=");

        /**
         * The symbol.
         */
        private String symbol;

        /**
         * Instantiates a new simple op.
         *
         * @param symbol the symbol
         */
        SimpleOp(String symbol) {
            this.symbol = symbol;
        }

        /**
         * Gets the symbol.
         *
         * @return the symbol
         */
        public String getSymbol() {
            return symbol;
        }

    }

    ;

    /**
     * The Class Simple.
     */
    public static final class Simple implements MCriteria {

        /**
         * The attr.
         */
        private final MAttribute attr;

        /**
         * The op.
         */
        private final SimpleOp op;

        /**
         * The rhs.
         */
        private final ModelElement<?> rhs;

        /**
         * Instantiates a new simple criteria.
         *
         * @param attr the attr
         * @param op   the op
         * @param rhs  the rhs
         */
        public Simple(MAttribute attr, SimpleOp op, ModelElement<?> rhs) {
            this.attr = attr;
            this.op = op;
            this.rhs = rhs;
        }

        /**
         * Gets the attribute.
         *
         * @return the attribute
         */
        public MAttribute getAttribute() {
            return attr;
        }

        /**
         * Gets the op.
         *
         * @return the op
         */
        public SimpleOp getOp() {
            return op;
        }

        /**
         * Gets the rhs.
         *
         * @return the rhs
         */
        public ModelElement<?> getRhs() {
            return rhs;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return attr + " " + op.getSymbol() + " " + rhs;
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MCriteria#asCriteria()
         */
        public Criteria asEhcacheObject() {
            switch (op) {
                case EQ:
                    return new EqualTo(attr.asEhcacheAttributeString(), getRhs().asEhcacheObject());
                case NE:
                    return new NotEqualTo(attr.asEhcacheAttributeString(), getRhs().asEhcacheObject());
                case GT:
                    return new GreaterThan(attr.asEhcacheAttributeString(), getRhs().asEhcacheObject());
                case LE:
                    return new LessThanOrEqual(attr.asEhcacheAttributeString(), getRhs().asEhcacheObject());
                case LT:
                    return new LessThan(attr.asEhcacheAttributeString(), getRhs().asEhcacheObject());
                case GE:
                    return new GreaterThanOrEqual(attr.asEhcacheAttributeString(), getRhs().asEhcacheObject());
            }
            throw new IllegalStateException("Unrecognized op: " + op);
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((attr == null) ? 0 : attr.hashCode());
            result = prime * result + ((op == null) ? 0 : op.hashCode());
            result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Simple other = (Simple)obj;
            if (attr == null) {
                if (other.attr != null) return false;
            } else if (!attr.equals(other.attr)) return false;
            if (op != other.op) return false;
            if (rhs == null) {
                if (other.rhs != null) return false;
            } else if (!rhs.equals(other.rhs)) return false;
            return true;
        }

    }

    /**
     * Model between criteria.
     */
    public static class Between implements MCriteria {

        /**
         * The attr.
         */
        private final MAttribute attr;

        /**
         * The min.
         */
        private final ModelElement<?> min;

        /**
         * The include min.
         */
        private final boolean includeMin;

        /**
         * The max.
         */
        private final ModelElement<?> max;

        /**
         * The include max.
         */
        private final boolean includeMax;

        /**
         * Instantiates a new between model criteria.
         *
         * @param attr       the attr
         * @param min        the min
         * @param includeMin the include min
         * @param max        the max
         * @param includeMax the include max
         */
        public Between(MAttribute attr, ModelElement<?> min, boolean includeMin, ModelElement<?> max, boolean includeMax) {
            this.attr = attr;
            this.min = min;
            this.includeMin = includeMin;
            this.max = max;
            this.includeMax = includeMax;
        }

        /**
         * Gets the attribute.
         *
         * @return the attribute
         */
        public MAttribute getAttribute() {
            return attr;
        }

        /**
         * Gets the min.
         *
         * @return the min
         */
        public ModelElement<?> getMin() {
            return min;
        }

        /**
         * Checks if is include min.
         *
         * @return true, if is include min
         */
        public boolean isIncludeMin() {
            return includeMin;
        }

        /**
         * Gets the max.
         *
         * @return the max
         */
        public ModelElement<?> getMax() {
            return max;
        }

        /**
         * Checks if is include max.
         *
         * @return true, if is include max
         */
        public boolean isIncludeMax() {
            return includeMax;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return attr + " between " + (includeMin ? "[" : "") + min + " and " + max + (includeMax ? "]" : "");
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MCriteria#asCriteria()
         */
        public Criteria asEhcacheObject() {
            return new net.sf.ehcache.search.expression.Between(attr.asEhcacheAttributeString(), getMin().asEhcacheObject(),
                getMax().asEhcacheObject(), isIncludeMin(), isIncludeMax());
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((attr == null) ? 0 : attr.hashCode());
            result = prime * result + (includeMax ? 1231 : 1237);
            result = prime * result + (includeMin ? 1231 : 1237);
            result = prime * result + ((max == null) ? 0 : max.hashCode());
            result = prime * result + ((min == null) ? 0 : min.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Between other = (Between)obj;
            if (attr == null) {
                if (other.attr != null) return false;
            } else if (!attr.equals(other.attr)) return false;
            if (includeMax != other.includeMax) return false;
            if (includeMin != other.includeMin) return false;
            if (max == null) {
                if (other.max != null) return false;
            } else if (!max.equals(other.max)) return false;
            if (min == null) {
                if (other.min != null) return false;
            } else if (!min.equals(other.min)) return false;
            return true;
        }

    }

    /**
     * The model ILike criteria.
     */
    public static class ILike implements MCriteria {

        /**
         * The attr.
         */
        private final MAttribute attr;

        /**
         * The regexp.
         */
        private final String regexp;

        /**
         * Instantiates a new i like.
         *
         * @param attr   the attr
         * @param regexp the regexp
         */
        public ILike(MAttribute attr, String regexp) {
            this.attr = attr;
            this.regexp = regexp;
        }

        /**
         * Gets the attribute.
         *
         * @return the attribute
         */
        public MAttribute getAttribute() {
            return attr;
        }

        /**
         * Gets the regexp.
         *
         * @return the regexp
         */
        public String getRegexp() {
            return regexp;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return attr + " ilike " + regexp;
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MCriteria#asCriteria()
         */
        public Criteria asEhcacheObject() {
            return new net.sf.ehcache.search.expression.ILike(attr.asEhcacheAttributeString(), getRegexp());
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((attr == null) ? 0 : attr.hashCode());
            result = prime * result + ((regexp == null) ? 0 : regexp.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            ILike other = (ILike)obj;
            if (attr == null) {
                if (other.attr != null) return false;
            } else if (!attr.equals(other.attr)) return false;
            if (regexp == null) {
                if (other.regexp != null) return false;
            } else if (!regexp.equals(other.regexp)) return false;
            return true;
        }
    }

    /**
     * The model Or.
     */
    public static class Or implements MCriteria {

        /**
         * The crits.
         */
        private MCriteria[] crits;

        /**
         * Instantiates a new or.
         *
         * @param crits the crits
         */
        public Or(MCriteria... crits) {
            this.crits = crits;
        }

        public MCriteria[] getCrits() {
            return crits;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            String s = crits[0].toString();
            for (int i = 1; i < crits.length; i++) {
                s = s + " or " + crits[i];
            }
            return "(" + s + ")";
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MCriteria#asCriteria()
         */
        public Criteria asEhcacheObject() {

            Criteria crit = new net.sf.ehcache.search.expression.Or(crits[crits.length - 2].asEhcacheObject(),
                crits[crits.length - 1].asEhcacheObject());
            if (crits.length > 2) {
                for (int i = crits.length - 3; i >= 0; i--) {
                    crit = new net.sf.ehcache.search.expression.Or(crits[i].asEhcacheObject(), crit);
                }
            }
            return crit;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(crits);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Or other = (Or)obj;
            if (!Arrays.equals(crits, other.crits)) return false;
            return true;
        }

    }

    /**
     * The Model And.
     */
    public static class And implements MCriteria {

        /**
         * The crits.
         */
        private final MCriteria[] crits;

        /**
         * Instantiates a new and.
         *
         * @param crits the crits
         */
        public And(MCriteria... crits) {
            this.crits = crits;
        }

        /**
         * Gets the criteria.
         *
         * @return the criteria
         */
        public MCriteria[] getCriteria() {
            return crits;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            String s = crits[0].toString();
            for (int i = 1; i < crits.length; i++) {
                s = s + " and " + crits[i];
            }
            return "(" + s + ")";
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MCriteria#asCriteria()
         */
        public Criteria asEhcacheObject() {
            Criteria crit = new net.sf.ehcache.search.expression.And(crits[crits.length - 2].asEhcacheObject(),
                crits[crits.length - 1].asEhcacheObject());
            if (crits.length > 2) {
                for (int i = crits.length - 3; i >= 0; i--) {
                    crit = new net.sf.ehcache.search.expression.And(crits[i].asEhcacheObject(), crit);
                }
            }
            return crit;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(crits);
            return result;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            And other = (And)obj;
            if (!Arrays.equals(crits, other.crits)) return false;
            return true;
        }

    }

    /**
     * The Model Not.
     */
    public static class Not implements MCriteria {

        /**
         * The crit.
         */
        private final MCriteria crit;

        /**
         * Instantiates a new not.
         *
         * @param crit1 the crit1
         */
        public Not(MCriteria crit1) {
            this.crit = crit1;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "(not " + crit + ")";
        }

        /**
         * Gets the criterium.
         *
         * @return the criterium
         */
        public MCriteria getCriterium() {
            return crit;
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MCriteria#asCriteria()
         */
        public Criteria asEhcacheObject() {
            return new net.sf.ehcache.search.expression.Not(crit.asEhcacheObject());
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((crit == null) ? 0 : crit.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Not other = (Not)obj;
            if (crit == null) {
                if (other.crit != null) return false;
            } else if (!crit.equals(other.crit)) return false;
            return true;
        }


    }


    public static class Like implements MCriteria {
        /**
         * The attr.
         */
        private final MAttribute attr;

        /**
         * The original regexp.
         */
        private final String originalRegexp;

        /**
         * Sanitized regex for like. Replace % with * and _ with ?
         */
        private final String sanitizedRegex;

        /**
         * Instantiates a new like.
         *
         * @param attr   the attr
         * @param regexp the regexp
         */
        public Like(MAttribute attr, String regexp) {
            this.attr = attr;
            this.originalRegexp = regexp;
            this.sanitizedRegex = regexp.replace('%', '*').replace('_', '?');
        }

        /**
         * Gets the attribute.
         *
         * @return the attribute
         */
        public MAttribute getAttribute() {
            return attr;
        }

        /**
         * Gets the original like formatted regexp.
         *
         * @return the regexp
         */
        public String getLikeRegex() {
            return originalRegexp;
        }

        /**
         * Gets the sanitized ilike formatted regexp.
         *
         * @return the regexp
         */
        public String getILikeRegex() {
            return sanitizedRegex;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return attr + " like " + originalRegexp;
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MCriteria#asCriteria()
         */
        public Criteria asEhcacheObject() {
            return new net.sf.ehcache.search.expression.ILike(attr.asEhcacheAttributeString(), getILikeRegex());
        }

        /*
        * (non-Javadoc)
        * @see java.lang.Object#hashCode()
        */
        @Override
        public int hashCode() {
            final int prime = 19;
            int result = 1;
            result = prime * result + ((attr == null) ? 0 : attr.hashCode());
            result = prime * result + ((originalRegexp == null) ? 0 : originalRegexp.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Like other = (Like)obj;
            if (attr == null) {
                if (other.attr != null) return false;
            } else if (!attr.equals(other.attr)) return false;
            if (originalRegexp == null) {
                if (other.originalRegexp != null) return false;
            } else if (!originalRegexp.equals(other.originalRegexp)) return false;
            return true;
        }


    }
}

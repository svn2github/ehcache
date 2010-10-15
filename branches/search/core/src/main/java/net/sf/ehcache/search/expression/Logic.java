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

/**
 * The purpose of this class is to allow static imports to make query building
 * look nicer in source code
 *
 * @author teck
 * @author Greg Luck
 */
public final class Logic {

    private Logic() {
        // static methods only
    }

    /**
     * Create a criteria for the logical "and" of the given criteria
     *
     * @param c the criteria to "and" together
     * @return an "and" criteria instance
     */
    public static Criteria and(Criteria... c) {
        return new And(c);
    }

    /**
     * Create a criteria for the logical "or" of the given criteria
     *
     * @param c the criteria to "or" together
     * @return an "or" criteria instance
     */
    public static Criteria or(Criteria... c) {
        return new Or(c);
    }

    /**
     * Create a criteria for the logical "not" of the given criteria
     *
     * @param c the criteria to "not" (ie. negate)
     * @return a "not" criteria instance
     */
    public static Criteria not(Criteria c) {
        return new Not(c);
    }

    /**
     * Create a criteria for the logical "not" of the given criteria
     *
     * @param c the criteria to "not" (ie. negate)
     * @return a "not" criteria instance
     */
//    public static Criteria between(Criteria c) {
//        return new Between(c);
//    }


}

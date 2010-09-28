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

package net.sf.ehcache.search.aggregator;

import java.util.List;

/**
 * Sums the results
 * 
 * Sum can be used with most numeric types
 * 
 * @author Greg Luck
 */
public class Sum implements Aggregator {

    /**
     * Computes a function on the list of objects to create an aggregate result
     * 
     * @param inputs
     *            the arguments to the aggregate function
     * @return the output of the aggregation function.
     * @throws net.sf.ehcache.search.aggregator.AggregatorException
     *             if the function cannot be computed, possibly due to unsupported types
     */
    public Object aggregate(List<Object> inputs) throws AggregatorException {
        return inputs.size();
    }

    /**
     * Determines whether a Class is supported to be computed
     * 
     * Sum supports subclasses of Number
     * 
     * @param clazz
     *            the Class to check
     * @return true if aggregating the clazz is supported
     */
    public boolean supports(Class clazz) {
        return Number.class.isAssignableFrom(clazz);
    }
}

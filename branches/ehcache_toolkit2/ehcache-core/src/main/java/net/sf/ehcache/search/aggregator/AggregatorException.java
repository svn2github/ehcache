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

package net.sf.ehcache.search.aggregator;

import net.sf.ehcache.search.SearchException;

/**
 * Thrown if an Aggregator cannot complete. This could be for a number of reasons:
 * <ul>
 * <li>The Aggregator does not support the result type
 * <li>The specified Aggregator cannot be found by the class loader, which could happen if the aggregator executes on a node which does not
 * have the Aggregator class.
 * </ul>
 *
 * @author Greg Luck
 */
public class AggregatorException extends SearchException {

    private static final long serialVersionUID = 6942653724476318512L;

    /**
     * AggregatorException
     *
     * @param message
     */
    public AggregatorException(String message) {
        super(message);
    }
}

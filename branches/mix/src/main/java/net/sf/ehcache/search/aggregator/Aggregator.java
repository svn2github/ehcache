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

/**
 * An Aggregator describes a requested aggregation for a query and provides aggregator instances for execution
 *
 * @author teck
 */
public interface Aggregator {

    /**
     * Create an instance of this aggregator. Every query execution will use a unique instance for requested aggregator
     *
     * @param <T>
     * @return aggregator instance
     */
    <T> AggregatorInstance<T> createInstance();

}

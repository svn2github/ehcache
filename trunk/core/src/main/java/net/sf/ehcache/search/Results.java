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

package net.sf.ehcache.search;

import java.util.List;

/**
 * Results object for an execution of a {@link Query}. Instances of this class
 * are thread safe
 *
 * @author teck
 * @author Greg Luck
 */
public interface Results {

    /**
     * Discard this query result. This call is not mandatory but can allow the
     * cache to immediately free any resources associated with this result.
     * Multiple calls are ignored. Attempting to read results from this instance
     * after this method has been called will produce {@link SearchException}
     * <p/>
     * NOTE: Not that it is defined yet, but it seems like we should implement whatever java7 ARM comes up here as well
     */
    void discard();

    /**
     * Retrieve all of the cache results in one shot. For large result sets this
     * might consume large amount of memory
     *
     * @return a List of all the matching cache entries
     * @throws SearchException
     */
    List<Result> all() throws SearchException;

    /**
     * Retrieve a subset of the cache results. This method is useful when
     * showing "paged" results in a user interface or simply to keep memory overhead fixed
     *
     * @param start  starting index to access
     * @param count the number of results to return
     * @return a List of the given size. This list will be smaller than the requested
     *         size if no more results are available. If there are no more results an empty
     *         list will be returned.
     * @throws SearchException
     */
    List<Result> range(int start, int count) throws SearchException, IndexOutOfBoundsException;

    /**
     * Retrieves the result of an aggregate function. If multiple aggregate functions were requested in this query, the a {@link List} of
     * the results will be returned (in the same order they were added to the original query)
     *
     * @throws SearchException
     */
    Object aggregateResult() throws SearchException;

    /**
     * Results size
     *
     * @return number of results present
     */
    int size();

    /**
     * Whether the Results have cache keys included
     *
     * @return true if keys included
     */
    boolean hasKeys();

    /**
     * Whether the results are an aggregate, in which case there is no list returned,
     * just a single value
     *
     * @return true if this is an aggregate
     */
    boolean isAggregate();

}

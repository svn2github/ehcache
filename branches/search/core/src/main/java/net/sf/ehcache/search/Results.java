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
     * showing "paged" results in a UI or simply to keep memory overhead fixed
     * 
     * @param start
     *            starting index to access
     * @param length
     *            length of the chunk to access
     * @return a List of the given size. This list might not match the requested
     *         size if no more results are available
     * @throws SearchException
     * @throws IndexOutOfBoundsException
     *             if the start index exceeds the
     *             result size or is negative, or if the startIndex + length exceeds
     *             the total size
     */
    List<Result> range(int start, int length) throws SearchException, IndexOutOfBoundsException;

    /**
     * Retrieves the result of an aggregate function.
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

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
     * Discard this query result. This call is not mandatory but is recommended after
     * the caller is done with results. It can allow the cache, which may be distributed,
     * to immediately free any resources associated with this result.
     * <p/>
     * Multiple calls are ignored. Attempting to read results from this instance after this method has been called will produce
     * {@link SearchException}
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
     * @param start starting index to access
     * @param count the number of results to return
     * @return a List of the given size. This list will be smaller than the requested
     *         size if no more results are available. If there are no more results an empty
     *         list will be returned.
     * @throws SearchException
     */
    List<Result> range(int start, int count) throws SearchException, IndexOutOfBoundsException;

    /**
     * Results size
     *
     * @return number of results present
     */
    int size();

    /**
     * Whether the Results have cache keys included.
     * If so these can be extracted from the Result object.
     *
     * @return true if keys are included
     */
    boolean hasKeys();

    /**
     * Whether the Results have cache values included.
     * If so these can be extracted from the Result object.
     *
     * @return true if values are included
     */
    boolean hasValues();

    /**
     * Whether the Results have cache attributes included.
     * If so these can be extracted from the Result object.
     *
     * @return true if attributes are included
     */
    boolean hasAttributes();

    /**
     * Whether the results contains aggregates
     *
     * @return true if this is an aggregate
     */
    boolean hasAggregators();

}

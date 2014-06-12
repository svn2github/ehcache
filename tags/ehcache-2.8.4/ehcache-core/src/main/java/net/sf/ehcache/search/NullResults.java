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

import java.util.Collections;
import java.util.List;

/**
 * An empty result instance
 *
 * @author teck
 */
public class NullResults implements Results {

    /**
     * A global instance that can be freely shared with the world since this type has no state
     */
    public static final NullResults INSTANCE = new NullResults();

    /**
     * {@inheritDoc}
     */
    public void discard() {
        //
    }

    /**
     * {@inheritDoc}
     */
    public List<Result> all() throws SearchException {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    public List<Result> range(int start, int count) throws SearchException, IndexOutOfBoundsException {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasKeys() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasValues() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAttributes() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAggregators() {
        return false;
    }

}

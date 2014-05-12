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

/**
 * Builder-style class that allows user to specify query execution preferences, if necessary
 * 
 * @author Terracotta
 */
public class ExecutionHints {
    /**
     * Default value
     */
    public static final int DEFAULT_RESULT_BATCH_SIZE = -1;
    
    private int batchSize = DEFAULT_RESULT_BATCH_SIZE;
    
    /**
     * Set desired batch size for search results. This may be used as a safeguard to keep memory overhead fixed,
     * when expecting total number of results to be large.
     * @param size
     * @return
     */
    public ExecutionHints setResultBatchSize(int size) {
        this.batchSize = size;
        return this;
    }
    
    /**
     * @return desired search result batch size
     */
    public int getResultBatchSize() {
        return batchSize;
    }
}

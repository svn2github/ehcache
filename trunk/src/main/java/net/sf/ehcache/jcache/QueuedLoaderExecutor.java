/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.jcache;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;


/**
 * A QueuedExecutor which uses a thread pool to schedule loads in the order in which they are requested
 * @author Greg Luck
 * @version $Id$
 */
public class QueuedExecutor extends ThreadPoolExecutor {
    private static final int KEEP_ALIVE_TIME = 100000;
    private static final int MAXIMUM_POOL_SIZE = 10;

    /**
     * Creates a new instance with 0 to max 10 threads in the pool and a queue limited to {@link Integer#MAX_VALUE}
     */
    public QueuedExecutor() {

        super(0, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());

    }


}

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

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.Future;
import junit.framework.TestCase;

/**
 * @author Greg Luck
 * @version $Id$
 *          Tests for QueuedExecutor
 */
public class QueuedExecutorTest extends TestCase {


    /**
     * Tests the QueuedExecutor with a single item
     */
    public void testQueuedExecutor() throws InterruptedException, ExecutionException {

        QueuedExecutor queuedExecutor = new QueuedExecutor();
        Future future = queuedExecutor.submit(new Callable() {

            /**
             * Computes a result, or throws an exception if unable to do so.
             *
             * @return computed result
             * @throws Exception if unable to compute a result
             */
            public Object call() throws Exception {
                return Boolean.TRUE;
            }
        });
        assertTrue(((Boolean) future.get()).booleanValue());
        
    }


    /**
     * Tests the QueuedExecutor with a single item
     */
    public void testMultipleQueuedExecutor() throws InterruptedException, ExecutionException {


        QueuedExecutor queuedExecutor = new QueuedExecutor();
        Future[] futures = new Future[100];

        for (int i = 0; i < 100; i++) {

            final int i1 = i;
            futures[i] = queuedExecutor.submit(new Callable() {

                /**
                 * Computes a result, or throws an exception if unable to do so.
                 *
                 * @return computed result
                 * @throws Exception if unable to compute a result
                 */
                public Object call() throws Exception {
                    return new Integer(i1);
                }
            });
        }

        for (int i = 0; i < 100; i++) {

            assertTrue(futures[0] != null);
        }


    }
}

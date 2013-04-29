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

package net.sf.ehcache;


import static org.junit.Assert.assertTrue;
import org.junit.Test;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test cases for status.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class StatusPerfTest {

    private static final Logger LOG = LoggerFactory.getLogger(StatusPerfTest.class.getName());

    private static int int1 = 1;
    private int int2 = 2;
    private Status status1 = Status.STATUS_ALIVE;

    /**
     * The status is checked in almost every public method.
     * It has to be fast.
     * This test keeps it that way.
     */
    @Test
    public void testEqualsPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.getElapsedTime();


        Status status2 = Status.STATUS_SHUTDOWN;

        for (int i = 0; i < 10000; i++) {
            status1.equals(status2);
        }
        stopWatch.getElapsedTime();
        for (int i = 0; i < 10000; i++) {
            status1.equals(status2);
        }
        long statusCompareTime = stopWatch.getElapsedTime();
        LOG.info("Time to do equals(Status): " + statusCompareTime);
        assertTrue("Status compare is greater than permitted time", statusCompareTime < 35);

    }

    /**
     * An alternate implementation that is and override of the equals in Object. This would not normally
     * be used
     */
    @Test
    public void testObjectEqualsPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.getElapsedTime();

        Object object = new Object();
        for (int i = 0; i < 10000; i++) {
            status1.equals(object);
        }
        stopWatch.getElapsedTime();
        for (int i = 0; i < 10000; i++) {
            status1.equals(object);
        }
        long objectCompareTime = stopWatch.getElapsedTime();
        LOG.info("Time to do equals(Object): " + objectCompareTime);
        assertTrue("Status compare is greater than permitted time", objectCompareTime < 25);


    }


    /**
     * This was the implementation up to ehcache 1.2
     */
    @Test
    public void testIntEqualsPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.getElapsedTime();

        int2 = 12;
        boolean result;
        for (int i = 0; i < 10000; i++) {
            result = int1 == int2;
        }
        stopWatch.getElapsedTime();
        for (int i = 0; i < 10000; i++) {
            result = int1 == int2;
        }
        long intCompareTime = stopWatch.getElapsedTime();
        LOG.info("Time to do int == int: " + intCompareTime);
        assertTrue("Status compare is greater than permitted time", intCompareTime < 10);


    }


}

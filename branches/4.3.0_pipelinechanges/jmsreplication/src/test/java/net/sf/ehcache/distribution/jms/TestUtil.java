/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.distribution.jms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Common fields and methods required by most test cases
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class TestUtil {

    /**
     * Where the config is
     */
    public static final String SRC_CONFIG_DIR = "src/main/config/";

    /**
     * Where the test config is
     */
    public static final String TEST_CONFIG_DIR = "src/test/resources/";
    /**
     * Where the test classes are compiled.
     */
    public static final String TEST_CLASSES_DIR = "target/test-classes/";


    private static final Logger LOG = Logger.getLogger(TestUtil.class.getName());


    /**
     * Force the VM to grow to its full size. This stops SoftReferences from being reclaimed in favour of
     * Heap growth. Only an issue when a VM is cold.
     */
    public static void forceVMGrowth() {
        allocateFiftyMegabytes();
        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //
        }
        System.gc();
    }

    public static void allocateFiftyMegabytes() {
        byte[] forceVMGrowth = new byte[40000000];
    }

    /**
     * @param name
     * @throws java.io.IOException
     */
    public static void deleteFile(String name) throws IOException {
        String diskPath = System.getProperty("java.io.tmpdir");
        final File diskDir = new File(diskPath);
        File dataFile = new File(diskDir, name + ".data");
        if (dataFile.exists()) {
            dataFile.delete();
        }
        File indexFile = new File(diskDir, name + ".index");
        if (indexFile.exists()) {
            indexFile.delete();
        }
    }

    /**
     * Measure memory used by the VM.
     *
     * @return
     * @throws InterruptedException
     */
    public static long measureMemoryUse() throws InterruptedException {
        System.gc();
        Thread.sleep(2000);
        System.gc();
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Runs a set of threads, for a fixed amount of time.
     */
    public static void runThreads(final List executables) throws Exception {

        final long endTime = System.currentTimeMillis() + 10000;
        final List<Throwable> errors = new ArrayList<Throwable>();

        // Spin up the threads
        final Thread[] threads = new Thread[executables.size()];
        for (int i = 0; i < threads.length; i++) {
            final Executable executable = (Executable) executables.get(i);
            threads[i] = new Thread() {
                public void run() {
                    try {
                        // Run the thread until the given end time
                        while (System.currentTimeMillis() < endTime) {
                            executable.execute();
                        }
                    } catch (Throwable t) {
                        // Hang on to any errors
                        errors.add(t);
                        LOG.info(t.getMessage());
                    }
                }
            };
            threads[i].start();
        }

        // Wait for the threads to finish
        for (Thread thread : threads) {
            thread.join();
        }

        // Throw any error that happened
        if (errors.size() != 0) {
            for (Throwable error : errors) {
                LOG.log(Level.SEVERE, error.getMessage(), error);
            }
            throw new Exception("Test thread failed with " + errors.size() + " exceptions.");
        }
    }

    /**
     * A runnable, that can throw an exception.
     */
    public interface Executable {
        /**
         * Executes this object.
         *
         * @throws Exception
         */
        void execute() throws Exception;
    }

    /**
     * A timer service used to check performance of tests.
     * <p/>
     * To enable this to work for different machines the following is done:
     * <ul>
     * <li>SimpleLog is used for logging with a known logging level controlled by <code>simplelog.properties</code>
     * which is copied to the test classpath. This removes logging as a source of differences.
     * Messages are sent to stderr which also makes it easy to see messages on remote continuous integration
     * machines.
     * <li>A speedAdjustmentFactor is used to equalize machines. It is supplied as a the System Property
     * 'net.sf.ehcache.speedAdjustmentFactor=n', where n is the number of times the machine is slower
     * than the reference machine e.g. 1.1. This factor is then used to adjust "elapsedTime"
     * as returned by this class. Elapsed Time is therefore not true time, but notional time equalized with the reference
     * machine. If you get performance tests failing add this property.
     * </ul>
     *
     * @author Greg Luck
     * @version $Id$
     *          A stop watch that can be useful for instrumenting for performance
     */
    public static class StopWatch {


        private static final String SUFFIX = "ms";


        /**
         * Used for performance benchmarking
         */
        private long timeStamp = System.currentTimeMillis();


        /**
         * Gets the time elapsed between now and for the first time, the creation
         * time of the class, and after that, between each call to this method
         * <p/>
         * Note this method returns notional time elapsed. See class description
         */
        public long getElapsedTime() {
            long now = System.currentTimeMillis();
            long elapsed = now - timeStamp;
            timeStamp = now;
            return elapsed;
        }

        /**
         * @return formatted elapsed Time
         */
        public String getElapsedTimeString() {
            return String.valueOf(getElapsedTime()) + SUFFIX;
        }

    }


}



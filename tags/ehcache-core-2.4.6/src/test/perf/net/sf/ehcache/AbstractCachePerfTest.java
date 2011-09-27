package net.sf.ehcache;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @author Alex Snaps
 */
public class AbstractCachePerfTest {

    public static final String TEST_CONFIG_DIR = "src/test/perfResources/";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCachePerfTest.class.getName());
    protected CacheManager manager;

    /**
     * setup test
     */
    @Before
    public void setUp() throws Exception {
        manager = CacheManager.create();
    }

    /**
     * teardown
     */
    @After
    public void tearDown() throws Exception {
        if (manager != null) {
            manager.shutdown();
        }
    }

      /**
   * Runs a set of threads, for a fixed amount of time.
   *
   * Does not fail if throwables are thrown.
   * @param executables the list of executables to execute
   * @param explicitLog whether to log detailed AsserttionErrors or not
   * @return the number of Throwables thrown while running
   */
    protected int runThreadsNoCheck(final List executables, final boolean explicitLog) throws Exception {

        final long endTime = System.currentTimeMillis() + 10000;
        final List<Throwable> errors = new ArrayList<Throwable>();

        // Spin up the threads
        final Thread[] threads = new Thread[executables.size()];
        for (int i = 0; i < threads.length; i++) {
            final Executable executable = (Executable) executables.get(i);
            threads[i] = new Thread() {
                @Override
                public void run() {
                    try {
                        // Run the thread until the given end time
                        while (System.currentTimeMillis() < endTime) {
                            Assert.assertNotNull(executable);
                            executable.execute();
                        }
                    } catch (Throwable t) {
                        // Hang on to any errors
                        errors.add(t);
                        if (!explicitLog && t instanceof AssertionError) {
                            LOG.info("Throwable " + t + " " + t.getMessage());
                        } else {
                            LOG.error("Throwable " + t + " " + t.getMessage(), t);
                        }
                    }
                }
            };
            threads[i].start();
        }

        // Wait for the threads to finish
        for (Thread thread : threads) {
            thread.join();
        }

//        if (errors.size() > 0) {
//            for (Throwable error : errors) {
//                LOG.info("Error", error);
//            }
//        }
        return errors.size();
    }

    /**
     * Measure memory used by the VM.
     *
     * @return
     * @throws InterruptedException
     */
    protected long measureMemoryUse() throws InterruptedException {
        System.gc();
        Thread.sleep(2000);
        System.gc();
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Runs a set of threads, for a fixed amount of time.
     *
     * Throws an exception if there are throwables during the run.
     */
    protected void runThreads(final List executables) throws Exception {
        int failures = runThreadsNoCheck(executables);
            LOG.info(failures + " failures");
            //CHM does have the occasional very slow time.
            assertTrue("Failures = " + failures, failures <= 35);
    }


    /**
     * Runs a set of threads, for a fixed amount of time.
     *
     * Does not fail if throwables are thrown.
     * @return the number of Throwables thrown while running
     */
    protected int runThreadsNoCheck(final List executables) throws Exception {
      return runThreadsNoCheck(executables, false);
    }

    /**
     * @param name
     * @throws java.io.IOException
     */
    protected void deleteFile(String name) throws IOException {
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


    protected interface Executable {
        /**
         * Executes this object.
         *
         * @throws Exception
         */
        void execute() throws Exception;
    }

    /**
     * Obtains an MBeanServer, which varies with Java version
     *
     * @return
     */
    public MBeanServer createMBeanServer() {
        try {
            Class managementFactoryClass = Class.forName("java.lang.management.ManagementFactory");
            Method method = managementFactoryClass.getMethod("getPlatformMBeanServer", (Class[]) null);
            return (MBeanServer) method.invoke(null, (Object[]) null);
        } catch (Exception e) {
            LOG.info("JDK1.5 ManagementFactory not found. Falling back to JMX1.2.1", e);
            return MBeanServerFactory.createMBeanServer("SimpleAgent");
        }
    }

    /**
     * Force the VM to grow to its full size. This stops SoftReferences from being reclaimed in favour of
     * Heap growth. Only an issue when a VM is cold.
     */
    static public void forceVMGrowth() {
        allocateFiftyMegabytes();
        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //
        }
        System.gc();
    }

    private static void allocateFiftyMegabytes() {
        byte[] forceVMGrowth = new byte[50000000];
    }
    
}

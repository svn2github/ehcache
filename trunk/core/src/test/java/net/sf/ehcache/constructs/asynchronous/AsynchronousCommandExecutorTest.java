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

package net.sf.ehcache.constructs.asynchronous;


import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.CacheManager;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Tests of the package.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class AsynchronousCommandExecutorTest {

    private static final Logger LOG = Logger.getLogger(AsynchronousCommandExecutorTest.class.getName());

    private static List messages = new ArrayList();

    /**
     * This counter is accessed by 50 threads, thus it is marked volatile so the VM takes more care
     * incrementing it.
     */
    private volatile int counter;


    private Ehcache messageCache;

    /**
     * Does setup tasks
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        messages = new ArrayList();
        AsynchronousCommandExecutor.getInstance().setUnsafeDispatcherThreadIntervalSeconds(2);
        messageCache = AsynchronousCommandExecutor.getInstance().getMessageCache();
        messageCache.removeAll();
        messages.clear();
    }

    @AfterClass
    public static void tearDown() throws AsynchronousCommandException {
        AsynchronousCommandExecutor.getInstance().dispose();
        CacheManager.getInstance().shutdown();
    }


    /**
     * Getter
     */
    public static List getMessages() {
        return messages;
    }


    /**
     * Send a test message to the Topic
     */
    public String appendTextMessage() throws Exception {
        ListAppenderCommand command = new ListAppenderCommand("test", 0, null);
        return AsynchronousCommandExecutor.getInstance().queueForExecution(command);
    }


    /**
     * Send a non-string Java serializable message to the Topic. Should be treated as an Object Message
     */
    public String sendSerializableMessage() throws AsynchronousCommandException, CacheException, InterruptedException {
        ListAppenderCommand command = new ListAppenderCommand(new IsSerializable(), 0, null);
        return AsynchronousCommandExecutor.getInstance().queueForExecution(command);
    }

    /**
     * A negative test which normally fails trying to send a Java non-serializable message to the Topic.
     * <p/>
     * The message will cause an exception at one of two places depending on timing:
     * <ol>
     * <li>DiskStore spoolThreadMain - if the element overflows to the disk cache
     * <li>SynchronousPublisher publishMessage - if the dispatcherThread picks it up to publish
     * </ol>
     */
    public String sendNonSerializableMessage() throws Exception {
        ListAppenderCommand command = new ListAppenderCommand(new NonSerializable(), 0, null);
        return AsynchronousCommandExecutor.getInstance().queueForExecution(command);
    }

    private void waitForProcessing() throws InterruptedException {
        Thread.sleep((long) (1700 + (200 * StopWatch.getSpeedAdjustmentFactor())));
    }

    private void assertNoCommandsInCache() throws CacheException {
        assertEquals(1, messageCache.getSize());
    }


    /**
     * Send a non-string Java serializable message to the Topic. Should be treated as an Object Message
     */
    @Test
    public void testSendSerializableMessage() throws AsynchronousCommandException, CacheException, InterruptedException {
        ListAppenderCommand command = new ListAppenderCommand(new IsSerializable(), 0, null);
        AsynchronousCommandExecutor.getInstance().queueForExecution(command);
        waitForProcessing();
        assertEquals(1, messages.size());
        assertNoCommandsInCache();
    }

    /**
     * Can we send messages asynchronously? Do they all get through and is the cache empty at the end?
     */
    @Test
    public void testAsynchronousSerializableCommandExecution() throws Exception {
        ListAppenderCommand command = new ListAppenderCommand(new IsSerializable(), 10, null);

        for (int i = 0; i < 20; i++) {
            AsynchronousCommandExecutor.getInstance().queueForExecution(command);
        }
        waitForProcessing();
        Thread.sleep(1000);
        assertEquals(20, messages.size());
        assertNoCommandsInCache();
    }

    /**
     * Can we send messages asynchronously? Do they all get through and is the cache empty at the end?
     */
    @Test
    public void testThreadingAsynchronousSerializableCommandExecution() throws Exception {
        ListAppenderCommand command = new ListAppenderCommand(new IsSerializable(), 0, null);

        for (int i = 0; i < 12; i++) {
            AsynchronousCommandExecutor.getInstance().queueForExecution(command);
        }
        int count = 0;
        while ((count = AsynchronousCommandExecutor.getInstance().countCachedPublishCommands()) != 0) {
            LOG.log(Level.INFO, "waiting: count is: " + count);
        }
        waitForProcessing();
        assertEquals(12, messages.size());
        assertNoCommandsInCache();
    }

    /**
     * Demonstrates the behaviour with bad messages. Each should be tried three times before the next message
     * is tried. This will take the thread interval which is overriden for testing purposes to 1 second * the
     * number of messages * 3 repeats + 1 = 16 seconds.
     */
    @Test
    public void testAsynchronousNonSerializableCommandExecution() throws Exception {
        for (int i = 0; i < 12; i++) {
            sendNonSerializableMessage();
        }
        waitForProcessing();
        assertEquals(0, messages.size());
    }


    /**
     * Show that, no matter how long the dispatcher thread interval is, messages will attempt to
     * be sent immediately.
     * <p/>
     * This will not work if the thread does polling. It must be able to be woken up when new messages
     * arrive for processing.
     */
    @Test
    public void testMessageSentImmediately() throws Exception {
        //Set the interval high and then wait for the initial 1 second interval to complete.
        AsynchronousCommandExecutor.getInstance().setDispatcherThreadIntervalSeconds(1000000);
        Thread.sleep(6000);
        sendSerializableMessage();
        Thread.sleep(2000);
        sendSerializableMessage();
        //Wait for messages to be sent.
        Thread.sleep(2000);
        //Will not work unless we were able to wake the thread up.
        assertEquals(2, messages.size());
        assertNoCommandsInCache();
    }


    /**
     * Tests that good messages still go through, and in the right order.
     *
     * @throws Exception
     */
    @Test
    public void testMixOfGoodAndBadMessages() throws Exception {

        for (int i = 0; i < 2; i++) {
            sendSerializableMessage();
            //sendNonSerializableMessage();
        }
        //Wait for messages to be sent.
        Thread.sleep(9000);
        assertEquals(2, messages.size());
        assertNoCommandsInCache();

    }

    /**
     * Should ignore attempts to send messages if they are not due.
     */
    @Test
    public void testMessagesNotRetriedBeforeAllowed() throws Exception {
        ListAppenderCommand command = new ListAppenderCommand(new IsSerializable(), 0, Exception.class);
        AsynchronousCommandExecutor commandExecutor = AsynchronousCommandExecutor.getInstance();
        String uid = commandExecutor.queueForExecution(command);
        Thread.sleep(4000);

        int attempts = commandExecutor.getExecuteAttemptsForCommand(uid);
        //All attempts after the first one should be ignored and not even registered
        assertEquals(1, attempts);
        //Should still be in cache
        assertEquals(2, messageCache.getSize());
    }


    /**
     * Multi-threaded command load/stability test
     * <p/>
     * 50 threads use the executor at the same time. We check that all messages came through
     * and no errors occurred;
     */
    @Test
    public void testConcurrentExecutors() throws Exception {

        // Run a set of threads that get, put and remove an entry
        final List executables = new ArrayList();
        for (int i = 0; i < 50; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    sendSerializableMessage();
                    counter++;
                }
            };
            executables.add(executable);
        }

        runThreads(executables);
        waitForProcessing();
        int count = messages.size();
        assertEquals(counter, count);

    }


    /**
     * Runs a set of threads, for a fixed amount of time.
     */
    protected void runThreads(final List executables) throws Exception {

        final long endTime = System.currentTimeMillis() + 10000;
        final Throwable[] errors = new Throwable[1];

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
                            Thread.sleep(2000);
                        }
                    } catch (Throwable t) {
                        // Hang on to any errors
                        errors[0] = t;
                    }
                }
            };
            threads[i].start();

        }

        long time = (long) System.currentTimeMillis();

        // Wait for the threads to finish
        int maximumWait = 30000;
        for (int i = 0; i < threads.length; i++) {
            threads[i].join(maximumWait);
            if (System.currentTimeMillis() >= time + maximumWait) {
                LOG.log(Level.SEVERE, "Killed Threads after timeout");
            }
        }

        // Throw any error that happened
        if (errors[0] != null) {
            throw new Exception("Test thread failed.", errors[0]);
        }
    }

    /**
     * A simple command pattern implementation, to allow single methods to be encapsulated and executed.
     */
    protected interface Executable {
        /**
         * Executes this object.
         */
        void execute() throws Exception;
    }


}

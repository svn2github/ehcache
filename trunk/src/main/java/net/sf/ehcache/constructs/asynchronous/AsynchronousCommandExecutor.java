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

package net.sf.ehcache.constructs.asynchronous;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.rmi.dgc.VMID;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * Handles the asynchronous execution of commands. This class contains subtle threading interactions and should
 * not be modified without comprehensive multi-threaded tests.
 * <p/>
 * AsynchronousCommandExecutor is a singleton. Multiple clients may use it. It will execute commands in the order they were
 * added per client. To preserve order, if a command cannot be executed, all commands will wait behind it.
 * <p/>
 * This code requires JDK1.5 at present.
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public final class AsynchronousCommandExecutor {
    /**
     * The name of the message cache in the ehcache.xml configuration file.
     */
    public static final String MESSAGE_CACHE = "net.sf.ehcache.constructs.asynchronous.MessageCache";

    /**
     * The command completed successfully
     */
    public static final String SUCCESSFUL_EXECUTION = "Successful execution";

    /**
     * The dispatcher thread interval. It wakes up the dispatcher thread and attempts to process commands in the cache.
     * Commands will ignore the execution request if they have a set time between retries. New messages dispatched, will
     * also cause commands to be attempted immediately.
     * <p/>
     * Setting this to a low value will cause high cpu load. The recommended value is the amount of time between failed
     * message retries, which by default is 1 minute.
     */
    public static final int DEFAULT_DISPATCHER_THREAD_INTERVAL_SECONDS = 60;

    /**
     * Minimum setting for the dispatcher thread interval.
     *
     * @see #DEFAULT_DISPATCHER_THREAD_INTERVAL_SECONDS
     */
    public static final int MINIMUM_SAFE_DISPATCHER_THREAD_INTERVAL = 30;

    /**
     * The messageCache contains {@link Command} element values, and a queue that maintains their order.
     * This is the key of the queue element.
     */
    public static final String QUEUE_KEY = "QueueKey";

    private static final long WAIT_FOR_THREAD_INITIALIZATION = 5;    

    private static final Log LOG = LogFactory.getLog(AsynchronousCommandExecutor.class.getName());
    private static final int MS_PER_SECOND = 1000;
    private static AsynchronousCommandExecutor singleton;
    private static CacheManager cacheManager;
    private boolean active;
    private Thread dispatcherThread;

    /**
     * The thread interval in seconds. Do not set this to 0 or too small a value or CPU usage will skyrocket.
     */
    private long dispatcherThreadIntervalSeconds;


    private AsynchronousCommandExecutor() throws CacheException {
        cacheManager = CacheManager.getInstance();
        addShutdownHook();
        active = true;
        dispatcherThreadIntervalSeconds = DEFAULT_DISPATCHER_THREAD_INTERVAL_SECONDS;
        dispatcherThread = new DispatcherThread();
        dispatcherThread.start();
        //wait for all the threads to initialize. Without this, if a command is immediately queued the notifyAll
        //on queueForExecution, the despatcher thread is not ready to receive it.
        try {
            Thread.sleep(WAIT_FOR_THREAD_INITIALIZATION);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while initiliazing", e);
        }

    }

    /**
     * Factory method to get an instance of MessageDispatcher.
     *
     * @return a fully initialized reference to the singleton.
     * @throws AsynchronousCommandException
     */
    public static synchronized AsynchronousCommandExecutor getInstance() throws AsynchronousCommandException {
        if (singleton == null) {
            try {
                singleton = new AsynchronousCommandExecutor();
            } catch (CacheException e) {
                throw new AsynchronousCommandException("Cannot create CacheManager. Detailed message is: "
                        + e.getMessage(), e);
            }

        }
        return singleton;
    }

    /**
     * Must be synchronized as potentially two threads could create new queues at the same time, with the result
     * that one element would be lost.
     *
     * @return the queue of messages, or if none existed, a new queue
     * @throws AsynchronousCommandException
     */
    synchronized LinkedList getQueue() throws AsynchronousCommandException {
        LinkedList queue;
        Ehcache cache = getMessageCache();
        Element element;
        try {
            element = cache.get(QUEUE_KEY);
        } catch (CacheException e) {
            throw new AsynchronousCommandException("Unable to retrieve queue.", e);
        }
        if (element == null) {
            queue = new LinkedList();
            Element queueElement = new Element(QUEUE_KEY, queue);
            cache.put(queueElement);
        } else {
            queue = (LinkedList) element.getValue();
        }
        return queue;
    }

    /**
     * Gets the message cache
     *
     * @return the {@link #MESSAGE_CACHE} cache
     * @throws AsynchronousCommandException if the {@link #MESSAGE_CACHE} is null
     */
    public Ehcache getMessageCache() throws AsynchronousCommandException {
        Ehcache cache = cacheManager.getEhcache(MESSAGE_CACHE);
        if (cache == null) {
            throw new AsynchronousCommandException(
                    "ehcache.xml with a configuration entry for " +
                            MESSAGE_CACHE + " was not found in the classpath.");
        }
        return cache;
    }

    /**
     * Stores parameters in the {@link #MESSAGE_CACHE} for later execution. A unique id is assigned to the
     * PublisherCommand and that id is enqueued. Values stored will persist across VM restarts, provided the
     * VM shutdown hooks have a chance to run.
     * <p/>
     * This method is synchronized because the underlying Queue implementation is not threadsafe.
     *
     * @param command the {@link Command} which will be called on to publish the message
     * @return the unique identifier for the command
     * @throws AsynchronousCommandException
     */
    public synchronized String queueForExecution(Command command) throws AsynchronousCommandException {
        InstrumentedCommand instrumentedCommand = new InstrumentedCommand(command);
        String uid = storeCommandToCache(instrumentedCommand);
        enqueue(uid);
        notifyAll();
        return uid;
    }

    private void enqueue(String uid) throws AsynchronousCommandException {
        Queue queue;
        queue = getQueue();
        queue.add(uid);
    }

    /**
     * Gets the number of attempts for the command so far
     *
     * @param uid - the unique id for the command returned from {@link #queueForExecution(Command)}
     * @return the number of times the command was executed
     * @throws CommandNotFoundInCacheException
     *                                      if the command was not found in the cache.
     * @throws AsynchronousCommandException if their is a problem accessing the cache.
     */
    public synchronized int getExecuteAttemptsForCommand(String uid) throws CommandNotFoundInCacheException,
            AsynchronousCommandException {
        InstrumentedCommand instrumentedCommand = retrieveInstrumentedCommandFromCache(uid);
        if (instrumentedCommand == null) {
            throw new CommandNotFoundInCacheException("Command " + uid + " + was not found in the messageCache");
        }
        return instrumentedCommand.getExecuteAttempts();
    }

    /**
     * A background thread that executes commands
     */
    private class DispatcherThread extends Thread {
        public DispatcherThread() {
            super("Message Dispatcher Thread");
            //allow VM to exit with this thread running
            setDaemon(true);
        }

        /**
         * RemoteDebugger thread method.
         */
        public void run() {
            dispatcherThreadMain();
        }
    }

    /**
     * The main method for the expiry thread.
     * <p/>
     * Will run while the cache is active. After the cache shuts down
     * it will take the expiryThreadInterval to wake up and complete.
     * <p/>
     * Any exceptions are logged.
     */
    private synchronized void dispatcherThreadMain() {
        while (true) {
            try {
                //wait for new messages or retry interval.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("dispatcherThreadIntervalSeconds: " + dispatcherThreadIntervalSeconds);
                }
                wait(dispatcherThreadIntervalSeconds * MS_PER_SECOND);
            } catch (InterruptedException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("messageCache: Dispatcher thread interrupted on Disk Store.");
                }
                //Should only happen on dispose.
                return;
            }
            if (!active) {
                return;
            }
            executeCommands();
        }
    }

    /**
     * Dequeues messages and sends thems until a failure occurs, in which case, we wait until next time
     * to try again.
     * <p/>
     * Each message in the queue is tried. There are many many reasons why a message can fail. It may be unique to the message
     * such as a MessageFormatException, a non-serializable message and so on, in which case it may only affect that
     * message. Or it could be that the service is down. Attempts are made to itentify messages that can never be
     * delivered so that they can be deleted and hold up the queue. No messages can be sent out of order, or the queue
     * rules will be broken. Subscribers may also be reliant on getting the messages in the right order. e.g. a delete
     * or update can only happen after an insert for a given domain object.
     * <p/>
     * This method is synchronized so that it will always complete before a dispose occurs.
     */
    private synchronized void executeCommands() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("executeCommands invoked. " + countCachedPublishCommands() + " messages to be sent.");
        }
        LinkedList queue = null;
        InstrumentedCommand instrumentedCommand = null;
        try {
            queue = getQueue();
        } catch (AsynchronousCommandException e) {
            LOG.fatal("Unable to access the cache to retrieve commands. ", e);
        }
        Object object = null;
        while (true) {
            object = queue.peek();
            if (object == null) {
                break;
            }
            String uid = (String) object;
            try {
                try {
                    instrumentedCommand = retrieveInstrumentedCommandFromCache(uid);
                    instrumentedCommand.attemptExecution();
                    remove(queue, uid, SUCCESSFUL_EXECUTION);
                } catch (RetryAttemptTooSoonException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(e.getMessage(), e);
                    }
                    break;
                } catch (TooManyRetriesException e) {
                    remove(queue, uid, e.getMessage());
                } catch (CommandNotFoundInCacheException e) {
                    remove(queue, uid, e.getMessage());
                }
            } catch (Throwable throwable) {
                boolean match = checkIfRetryOnThrowable(throwable, instrumentedCommand);
                if (!match) {
                    remove(queue, uid, throwable.getMessage());
                } else {
                    //retry
                    if (LOG.isInfoEnabled()) {
                     LOG.info("Publishing attempt number " + instrumentedCommand.getExecuteAttempts()
                                + " failed. " + throwable.getMessage(), throwable);
                    }
                    break;
                }
            }
        }
    }


    private boolean checkIfRetryOnThrowable(Throwable throwable, InstrumentedCommand instrumentedCommand) {
        Command command = instrumentedCommand.command;
        Class[] retryThrowables = command.getThrowablesToRetryOn();
        if (retryThrowables == null) {
            return false;
        }
        boolean match = false;
        for (int i = 0; i < retryThrowables.length; i++) {
            Class retryThrowable = retryThrowables[i];
            if (retryThrowable.isInstance(throwable)) {
                match = true;
            }

        }
        return match;
    }

    private void remove(Queue queue, String uid, String reason) {
        queue.remove();
        Ehcache cache = null;
        try {
            cache = getMessageCache();
        } catch (AsynchronousCommandException e) {
            LOG.fatal("Unable to get cache + " + e.getMessage(), e);
        }
        cache.remove(uid);
        if (reason.equals(SUCCESSFUL_EXECUTION)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Deleting command with uid " + uid + ". " + reason);
            }
        } else {
            LOG.error("Deleting command with uid " + uid + ".  " + reason);
        }
    }


    private InstrumentedCommand retrieveInstrumentedCommandFromCache(String uid)
            throws CommandNotFoundInCacheException {
        Element element = null;
        try {
            //Cache not alive here. Why?
            Ehcache cache = getMessageCache();
            element = cache.get(uid);
        } catch (Exception e) {
            throw new CommandNotFoundInCacheException("Cache error while retrieving command", e);
        }

        if (element == null) {
            throw new CommandNotFoundInCacheException("Command " + uid + " not found in cache.");
        }
        return (InstrumentedCommand) element.getValue();
    }

    /**
     * Some caches might be persistent, so we want to add a shutdown hook if that is the
     * case, so that the data and index can be written to disk.
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                synchronized (this) {
                    if (active) {
                        LOG.info("VM shutting down with the MessageDispatcher active. There are "
                                + countCachedPublishCommands() +
                                " messages which will be cached to disk for delivery on VM restart.");
                        dispose();
                    }
                }
            }
        });
    }

    /**
     * @return the approximate number of PublishCommands stored in the cache
     */
    public synchronized int countCachedPublishCommands() {
        int messageCount = 0;
        try {
            Ehcache cache = getMessageCache();
            messageCount = cache.getSize();
        } catch (Exception e) {
            LOG.info("Unable to determine the number"
                    + " of messages in the messageCache.", e);
        }
        if (messageCount != 0) {
            //don't count queue, which should always be there.
            messageCount--;
        }
        return messageCount;
    }

    /**
     * ehcache also has a shutdown hook, so it will save all to disk.
     * <p/>
     * Shuts down the disk store in preparation for cache shutdown
     * <p/>
     * If a VM crash happens, the shutdown hook will not run. The data file and the index file
     * will be out of synchronisation. At initialisation we always delete the index file
     * after we have read the elements, so that it has a zero length. On a dirty restart, it still will have
     * and the data file will automatically be deleted, thus preserving safety.
     */
    public synchronized void dispose() {
        int messages = countCachedPublishCommands();
        LOG.info("Shutting down Message Dispatcher. " + messages + " messages remaining.");

        if (!active) {
            return;
        }
        try {
            if (dispatcherThread != null) {
                dispatcherThread.interrupt();
            }

        } catch (Exception e) {
            LOG.error("Could not shut down MessageDispatcher", e);
        } finally {
            active = false;
            notifyAll();
        }
    }

    /**
     * @param instrumentedCommand
     * @return A unique id which acts as a handle to the message
     * @throws AsynchronousCommandException
     */
    String storeCommandToCache(InstrumentedCommand instrumentedCommand) throws AsynchronousCommandException {
        String uid = generateUniqueIdentifier();
        Element element = new Element(uid, instrumentedCommand);
        Ehcache messageCache = getMessageCache();
        messageCache.put(element);
        return uid;
    }


    /**
     * Generates an ID that is guaranteed to be unique for all VM invocations on a machine with a
     * given IP address.
     *
     * @return A String representation of the unique identifier.
     */
    String generateUniqueIdentifier() {
        VMID guid = new VMID();
        return guid.toString();
    }

    /**
     * Sets the interval between runs of the dispatch thread, when no new dispatch invocations have occurred.
     *
     * @param dispatcherThreadIntervalSeconds
     *         the time in seconds
     * @throws IllegalArgumentException if the argument is less than 30
     * @see #DEFAULT_DISPATCHER_THREAD_INTERVAL_SECONDS for more information.
     */
    public void setDispatcherThreadIntervalSeconds(long dispatcherThreadIntervalSeconds)
            throws IllegalArgumentException {
        if (dispatcherThreadIntervalSeconds < MINIMUM_SAFE_DISPATCHER_THREAD_INTERVAL) {
            throw new IllegalArgumentException("Must be greater than 30 seconds to avoid high cpu load");
        }
        setUnsafeDispatcherThreadIntervalSeconds(dispatcherThreadIntervalSeconds);
    }

    /**
     * Sets the interval between runs of the dispatch thread, when no new dispatch invocations have occurred.
     * <p/>
     * Provided with package local access to permit testing
     *
     * @param dispatcherThreadIntervalSeconds
     *         the time in seconds
     * @see #DEFAULT_DISPATCHER_THREAD_INTERVAL_SECONDS for more information.
     */
    public void setUnsafeDispatcherThreadIntervalSeconds(long dispatcherThreadIntervalSeconds) {
        this.dispatcherThreadIntervalSeconds = dispatcherThreadIntervalSeconds;
    }

    /**
     * A <code>Command</code> instrumented with information about retry attempts
     */
    private final static class InstrumentedCommand implements Serializable {
        private Command command;

        /**
         * A record of the attempts to execute this command
         */
        private Stack executeAttempts;


        private InstrumentedCommand(Command command) {
            this.command = command;
            executeAttempts = new Stack();
        }

        /**
         * Records the data and time an execution attempt was made
         */
        private void registerExecutionAttempt() {
            Date date = new Date();
            executeAttempts.add(date);
        }

        private void attemptExecution() throws Throwable, TooManyRetriesException, RetryAttemptTooSoonException {
            checkAttemptNotTooSoon();
            checkNotTooManyAttempts();
            command.execute();
        }

        /**
         * Checks that enough time has elapsed to attempt an execution
         *
         * @throws RetryAttemptTooSoonException if sufficient time has not elapsed
         */
        private void checkAttemptNotTooSoon() throws RetryAttemptTooSoonException {
            //must guard against this because of the design of stack
            if (!executeAttempts.empty()) {
                Date lastAttempt = (Date) executeAttempts.peek();
                long delay = command.getDelayBetweenAttemptsInSeconds() * MS_PER_SECOND;
                Date nextAttemptDue = new Date(lastAttempt.getTime() + (delay));
                Date now = new Date();
                if (now.before(nextAttemptDue)) {
                    throw new RetryAttemptTooSoonException("Attempt to execute command before it is due is being ignored.");
                }
            }
        }

        /**
         * Checks that the number of attempts does not exceed the number of attempts defined in the command
         *
         * @throws TooManyRetriesException
         */
        private void checkNotTooManyAttempts() throws TooManyRetriesException {
            registerExecutionAttempt();
            if (getExecuteAttempts() > command.getNumberOfAttempts()) {
                throw new TooManyRetriesException("Retry attempt number " + getExecuteAttempts() + " is greater than "
                        + " the number permitted of " + command.getNumberOfAttempts()
                        + ".\n" + this);
            }
        }

        private int getExecuteAttempts() {
            //must guard against this because of the design of stack
            if (executeAttempts.empty()) {
                return 0;
            } else {
                return executeAttempts.size();
            }
        }


        /**
         * @return a string representation of the object.
         */
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("InstrumentedCommand: \n")
                    .append(super.toString())
                    .append("Previous Execution Attempts: \n");

            if (getExecuteAttempts() > 0) {
                for (int i = 0; i < getExecuteAttempts(); i++) {
                    Date date = (Date) executeAttempts.get(i);
                    buffer.append(date).append(" ");
                }
            }

            buffer.append("Command: \n") .append(command);
            return buffer.toString();
        }


    }


}




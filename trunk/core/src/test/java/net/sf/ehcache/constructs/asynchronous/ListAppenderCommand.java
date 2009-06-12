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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A test command
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
class ListAppenderCommand implements Command {

    private static final Logger LOG = LoggerFactory.getLogger(ListAppenderCommand.class.getName());

    private static final int NUMBER_OF_ATTEMPTS = 3;
    private static final int DELAY_IN_SECONDS = 1200;
    private Serializable payload;
    private long timeToCompleteInMs;
    private Class exceptionOnExecute;

    /**
     * Constructor
     */
    public ListAppenderCommand(Serializable payload, long timeToCompleteInMs, Class exceptionOnExecute) {
        this.payload = payload;
        this.timeToCompleteInMs = timeToCompleteInMs;
        this.exceptionOnExecute = exceptionOnExecute;

    }


    /**
     * Executes the command. The command is successful if it does not throw an exception.
     *
     * @throws Throwable A command could do anything and could throw any {@link Exception} or {@link Error}
     * @see #getThrowablesToRetryOn() to set {@link Throwable}s that should are expected
     */
    public void execute() throws Throwable {
        LOG.debug("About to attempt execution");
        checkSerializability(payload);
        Thread.sleep(timeToCompleteInMs);
        if (exceptionOnExecute != null) {
            throw (Throwable) exceptionOnExecute.newInstance();
        }
        AsynchronousCommandExecutorTest.getMessages().add(payload);
    }

    private void checkSerializability(Serializable payload) throws IOException {
        ObjectOutputStream oout = new ObjectOutputStream(new ByteArrayOutputStream());
        oout.writeObject(payload);
    }

    /**
     * The AsynchronousCommandExecutor may also be fault tolerant. This method returns a list of {@link Throwable}
     * classes such that if one if thrown during an execute attempt the command will simply retry after an interval
     * until it uses up all of its retry attempts. If a {@link Throwable} does occurs which is not in this list,
     * an {@link AsynchronousCommandException} will be thrown and the command will be removed.
     * <p/>
     *
     * @return a list of {@link Class}s. It only makes sense for the list to contain Classes which are subclasses
     *         of Throwable
     */
    public Class[] getThrowablesToRetryOn() {
        return new Class[]{Exception.class};
    }

    /**
     * @return the number of times the dispatcher should try to send the message. A non-zero value implies fault tolerance
     *         and only makes sense if {@link #getThrowablesToRetryOn()} is non-null.
     */
    public int getNumberOfAttempts() {
        return NUMBER_OF_ATTEMPTS;
    }

    /**
     * @return the delay between attempts, in seconds. A non-zero value implies fault tolerance
     *         and only makes sense if {@link #getThrowablesToRetryOn()} is non-null.
     */
    public int getDelayBetweenAttemptsInSeconds() {
        return DELAY_IN_SECONDS;
    }
}


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

import java.io.Serializable;


/**
 * An asynchronous encapsulated command. Callers do not need to know what the command does.
 * <p/>
 * Commands must be serializable, so that they be persisted to disk by ehcache.
 * <p/>
 * The command can also be fault tolerant. It is made fault tolerant when {@link #getThrowablesToRetryOn()}  is non null.
 * Any {@link Throwable}s thrown that are <<code>instanceof</code> a <code>Throwable</code> in the array are expected
 * and will result in reexecution up to the maximum number of attempts, after the delay between repeats.
 * allowing a delay each time.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public interface Command extends Serializable {

    /**
     * Executes the command. The command is successful if it does not throw an exception.
     * @throws Throwable A command could do anything and could throw any {@link Exception} or {@link Error}
     * @see #getThrowablesToRetryOn() to set {@link Throwable}s that should are expected
     */
    void execute() throws Throwable;

    /**
     * The AsynchronousCommandExecutor may also be fault tolerant. This method returns a list of {@link Throwable}
     * classes such that if one if thrown during an execute attempt the command will simply retry after an interval
     * until it uses up all of its retry attempts. If a {@link Throwable} does occurs which is not in this list,
     * an {@link AsynchronousCommandException} will be thrown and the command will be removed.
     * <p>
     * @return a list of {@link Class}s. It only makes sense for the list to contain Classes which are subclasses
     * of Throwable
     */
    Class[] getThrowablesToRetryOn();

    /**
     * @return  the number of times the dispatcher should try to send the message. A non-zero value implies fault tolerance
     * and only makes sense if {@link #getThrowablesToRetryOn()} is non-null.
     */
    int getNumberOfAttempts();

    /**
     * @return the delay between attempts, in seconds. A non-zero value implies fault tolerance
     * and only makes sense if {@link #getThrowablesToRetryOn()} is non-null.
     */
    int getDelayBetweenAttemptsInSeconds();


}

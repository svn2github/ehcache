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

package net.sf.ehcache.constructs.nonstop;

import java.util.concurrent.TimeoutException;

/**
 * Subclass of {@link TimeoutException}. This class is used to indicate a failure in NonStopCacheExecutorService.execute(..) when the task
 * was not submitted to the executor service at all.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class TaskNotSubmittedTimeoutException extends TimeoutException {

    private final int submitAttemptCount;

    /**
     * Default constructor
     */
    public TaskNotSubmittedTimeoutException() {
        this("", 0);
    }

    /**
     * Constructor accepting a message
     * 
     * @param message
     */
    public TaskNotSubmittedTimeoutException(String message) {
        this(message, 0);
    }

    /**
     * Constructor accepting number of attempts made when this exception happened
     * 
     * @param submitAttemptCount
     */
    public TaskNotSubmittedTimeoutException(int submitAttemptCount) {
        this("", submitAttemptCount);
    }

    /**
     * Constructor accepting message and number of attempts made
     * 
     * @param submitAttemptCount
     */
    public TaskNotSubmittedTimeoutException(String message, int submitAttemptCount) {
        super(message);
        this.submitAttemptCount = submitAttemptCount;
    }

    /**
     * Getter for submit attempts made.
     * 
     * @return number of submit attempts made
     */
    public int getSubmitAttemptCount() {
        return submitAttemptCount;
    }

}

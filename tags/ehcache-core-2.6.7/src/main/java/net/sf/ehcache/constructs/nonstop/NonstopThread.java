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

/**
 * Thread used for doing nonstop operations
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopThread extends Thread {

    /**
     * Public constructor
     *
     * @param target the runnable to execute
     * @param name name of the threads
     */
    public NonstopThread(Runnable target, String name) {
        super(target, name);
    }

    /**
     * Find out if current executing thread is a nonstop thread
     * @return true if current thread is an instance of this thread, otherwise returns false
     */
    public static boolean isCurrentThreadNonstopThread() {
        return Thread.currentThread() instanceof NonstopThread;
    }
}

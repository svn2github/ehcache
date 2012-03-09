/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.distribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A utility class for distributed tests
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class JVMUtil {

    /**
     * Utility class. No constructor
     */
    private JVMUtil() {
        //noop
    }


    /**
     * Lists all the threads in the VM
     *
     * @return a List of type Thread
     */
    public static Collection<Thread> enumerateThreads() {
        // Find the root thread group
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }

        Thread[] threads;
        do {
            int activeEstimate = root.activeCount();
            threads = new Thread[activeEstimate + 1];
        } while (root.enumerate(threads) >= threads.length);
        
        return new ArrayList<Thread>(Arrays.asList(threads));
    }

}

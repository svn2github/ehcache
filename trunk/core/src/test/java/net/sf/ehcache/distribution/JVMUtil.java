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

package net.sf.ehcache.distribution;

import java.util.List;
import java.util.ArrayList;

/**
 * A utility class for distributed tests
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class JVMUtil {
    private static final float JDK_1_5 = 1.5f;
    private static final int FIRST_THREE_CHARS = 3;

    /**
     * Utility class. No constructor
     */
    private JVMUtil() {
        //noop
    }


    /**
     * JDK Bug Id 4267864 affecting JDKs limits the number of RMI registries to one per virtual
     * machine. Because tests rely on creating multiple they will only work on JDK1.5.
     *
     * This method is used to not run the affected tests on JDK1.4.
     * @return true if the JDK is limited to one RMI Registry per VM, else false
     */
    public static boolean isSingleRMIRegistryPerVM() {
        String version = System.getProperty("java.version");
        String majorVersion = version.substring(0, FIRST_THREE_CHARS);
        float majorVersionFloat = Float.parseFloat(majorVersion);
        return majorVersionFloat < JDK_1_5;
    }


    /**
     * Some performance numbers and size limits are higher in 15.
     * This is used to set the tests as high as possible for each VM.
     * @return true if JDK1.5 or higher
     */
    public static boolean isJDK15() {
        return !isSingleRMIRegistryPerVM();
    }

    /**
     * Lists all the threads in the VM
     * @return a List of type Thread
     */
    public static List enumerateThreads() {

        /**
         * A class for visiting threads
         */
        class ThreadVisitor {

            private List threadList = new ArrayList();

            // This method recursively visits all thread groups under `group'.
            private void visit(ThreadGroup group, int level) {
                // Get threads in `group'
                int numThreads = group.activeCount();
                Thread[] threads = new Thread[numThreads * 2];
                numThreads = group.enumerate(threads, false);

                // Enumerate each thread in `group'
                for (int i = 0; i < numThreads; i++) {
                    // Get thread
                    Thread thread = threads[i];
                    threadList.add(thread);
                }

                // Get thread subgroups of `group'
                int numGroups = group.activeGroupCount();
                ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
                numGroups = group.enumerate(groups, false);

                // Recursively visit each subgroup
                for (int i = 0; i < numGroups; i++) {
                    visit(groups[i], level + 1);
                }
            }
        }

        // Find the root thread group
        ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
        while (root.getParent() != null) {
            root = root.getParent();
        }

        // Visit each thread group
        ThreadVisitor visitor = new ThreadVisitor();
        visitor.visit(root, 0);
        return visitor.threadList;
    }

}

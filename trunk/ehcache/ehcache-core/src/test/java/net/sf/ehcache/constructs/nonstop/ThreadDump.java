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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public abstract class ThreadDump {
    private static final String NEWLINE = System.getProperty("line.separator", "\n");

    public static List<ThreadInformation> getThreadDump() {
        List<ThreadInformation> rv = new ArrayList<ThreadInformation>();
        ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
        for (long id : tbean.getAllThreadIds()) {
            ThreadInfo tinfo = tbean.getThreadInfo(id);
            if (tinfo != null) {
                rv.add(new ThreadInformation(tinfo.getThreadId(), tinfo.getThreadName()));
            }
        }
        return rv;
    }

    public static String takeThreadDump() {
        StringBuilder rv = new StringBuilder();
        ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
        for (long id : tbean.getAllThreadIds()) {
            ThreadInfo tinfo = tbean.getThreadInfo(id, Integer.MAX_VALUE);
            if (tinfo != null) {
                rv.append(tinfo).append(NEWLINE);
                for (StackTraceElement e : tinfo.getStackTrace()) {
                    rv.append("    at ").append(e).append(NEWLINE);
                }
                rv.append(NEWLINE);
            }
        }
        return rv.toString();
    }

    public static class ThreadInformation {
        private final long threadId;
        private final String threadName;

        public ThreadInformation(long threadId, String name) {
            super();
            this.threadId = threadId;
            this.threadName = name;
        }

        public long getThreadId() {
            return threadId;
        }

        public String getThreadName() {
            return threadName;
        }

        @Override
        public int hashCode() {
            return ((int) threadId) ^ ((int) (threadId >>> 32));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof ThreadInformation) {
                return threadId == ((ThreadInformation) obj).threadId;
            } else {
                return false;
            }
        }

    }
}

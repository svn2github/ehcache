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

package net.sf.ehcache.constructs.nonstop;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public abstract class ThreadDump {

    public static List<ThreadInformation> getThreadDump() {
        List<ThreadInformation> rv = new ArrayList<ThreadInformation>();
        ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
        for (long id : tbean.getAllThreadIds()) {
            ThreadInfo tinfo = tbean.getThreadInfo(id, Integer.MAX_VALUE);
            rv.add(new ThreadInformation(tinfo.getThreadId(), tinfo.getThreadName()));
        }
        return rv;
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
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (threadId ^ (threadId >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ThreadInformation other = (ThreadInformation) obj;
            if (threadId != other.threadId)
                return false;
            return true;
        }

    }
}

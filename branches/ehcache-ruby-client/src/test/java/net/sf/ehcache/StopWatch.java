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

package net.sf.ehcache;

/**
 * @author Greg Luck
 * @version $Id$
 * A stop watch that can be useful for instrumenting for performance
 */
public class StopWatch {

    private static final String SUFFIX = "ms";
                                         
    /**
     * Used for performance benchmarking
     */
    private long timeStamp = System.currentTimeMillis();


    /**
     * Gets the time elapsed between now and for the first time, the creation
     * time of the class, and after that, between each call to this method
     */
    public long getElapsedTime() {
        long now = System.currentTimeMillis();
        long elapsed = now - timeStamp;
        timeStamp = now;
        return elapsed;
    }

    /**
     * @return formatted elapsed Time
     */
    public String getElapsedTimeString() {
        return String.valueOf(getElapsedTime()) + SUFFIX;
    }

}



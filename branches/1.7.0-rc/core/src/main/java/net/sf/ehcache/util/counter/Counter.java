/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.util.counter;

/**
 * A simple counter
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 * @since 1.7
 */
public interface Counter {

    /**
     * Increment the counter by 1
     * 
     * @return
     */
    long increment();

    /**
     * Decrement the counter by 1
     * 
     * @return
     */
    long decrement();

    /**
     * Returns the value of the counter and sets it to the new value
     * 
     * @param newValue
     * @return
     */
    long getAndSet(long newValue);

    /**
     * Gets current value of the counter
     * 
     * @return
     */
    long getValue();

    /**
     * Increment the counter by given amount
     * 
     * @param amount
     * @return
     */
    long increment(long amount);

    /**
     * Decrement the counter by given amount
     * 
     * @param amount
     * @return
     */
    long decrement(long amount);

    /**
     * Sets the value of the counter to the supplied value
     * 
     * @param newValue
     */
    void setValue(long newValue);

}

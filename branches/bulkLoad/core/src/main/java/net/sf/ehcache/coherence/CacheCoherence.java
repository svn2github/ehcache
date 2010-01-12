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

/**
 * 
 */
package net.sf.ehcache.coherence;

/**
 * Interface defining API for cache coherence.
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 *
 */
public interface CacheCoherence {

    /**
     * Returns true if the cache is in coherent mode cluster-wide.
     * 
     * @return true if the cache is in coherent mode cluster-wide.
     */
    public boolean isCoherent();

    /**
     * Sets the cache in coherent or incoherent mode depending on the parameter.
     * Calling {@code setCoherent(true)} when the cache is already in coherent mode or
     * calling {@code setCoherent(false)} when already in incoherent mode will be a no-op.
     * 
     * @param coherent
     *            true transitions to coherent mode, false to incoherent mode
     */
    public void setCoherent(boolean coherent);

    /**
     * This method waits until the cache is in coherent mode in all the connected nodes. If the cache is already in coherent mode it returns
     * immediately
     * 
     * It applies to coherent clustering mechanisms only e.g. Terracotta
     */
    public void waitUntilCoherent();

    /**
     * Returns true if the cache is in coherent mode for this node in the cluster. Returns false otherwise.
     * The cache can be in incoherent mode even when this method returns true as there may be other nodes
     * where the cache is currently not coherent. Use {@link #isCoherent()} to find out if the cache is
     * coherent cluster-wide.
     * <p />
     * It applies to coherent clustering mechanisms only e.g. Terracotta
     * 
     * 
     * @return true if the cache is in coherent mode in this node in the cluster, false otherwise
     */
    public boolean isCoherentLocally();
}

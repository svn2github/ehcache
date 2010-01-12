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

package net.sf.ehcache.coherence;

/**
 * A no-op implementation of {@link CacheCoherence}
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public final class NoopCacheCoherence implements CacheCoherence {

    /**
     * Singleton instance.
     */
    public static final CacheCoherence INSTANCE = new NoopCacheCoherence();

    /**
     * private constructor
     */
    private NoopCacheCoherence() {
        // private constructor, use the single static instance
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCoherent() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCoherentLocally() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setCoherent(boolean coherent) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilCoherent() {
        // no-op
    }

}

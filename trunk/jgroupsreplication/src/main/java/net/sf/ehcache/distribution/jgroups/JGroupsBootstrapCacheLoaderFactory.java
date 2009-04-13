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

package net.sf.ehcache.distribution.jgroups;

import net.sf.ehcache.bootstrap.BootstrapCacheLoaderFactory;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.util.PropertyUtil;

import java.util.Properties;
import java.util.logging.Logger;


/**
 * A factory to create a configured JGroupsBootstrapCacheLoader
 * @author Greg Luck
 * @version $Id$
 */
public class JGroupsBootstrapCacheLoaderFactory extends BootstrapCacheLoaderFactory {


    /**
     * The property name expected in ehcache.xml for the bootstrap asyncrhonously switch.
     */
    public static final String BOOTSTRAP_ASYNCHRONOUSLY = "bootstrapAsynchronously";

    /**
     * The property name expected in ehcache.xml for the maximum chunk size in bytes
     */
    public static final String MAXIMUM_CHUNK_SIZE_BYTES = "maximumChunkSizeBytes";

    /**
     * The default maximum serialized size of the elements to request from a remote cache peer during bootstrap.
     */
    protected static final int DEFAULT_MAXIMUM_CHUNK_SIZE_BYTES = 5000000;

    /**
     * The highest reasonable chunk size in bytes
     */
    protected static final int ONE_HUNDRED_MB = 100000000;

    /**
     * The lowest reasonable chunk size in bytes
     */
    protected static final int FIVE_KB = 5000;

    private static final Logger LOG = Logger.getLogger(JGroupsBootstrapCacheLoaderFactory.class.getName());


    /**
     * Create a <code>BootstrapCacheLoader</code>
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     * @return a constructed BootstrapCacheLoader
     */
    public BootstrapCacheLoader createBootstrapCacheLoader(Properties properties) {
        boolean bootstrapAsynchronously = extractBootstrapAsynchronously(properties);
        int maximumChunkSizeBytes = extractMaximumChunkSizeBytes(properties);
        return new JGroupsBootstrapCacheLoader(bootstrapAsynchronously, maximumChunkSizeBytes);
    }

    /**
     *
     * @param properties
     */
    protected int extractMaximumChunkSizeBytes(Properties properties) {
        int maximumChunkSizeBytes = 0;
        String maximumChunkSizeBytesString = PropertyUtil.extractAndLogProperty(MAXIMUM_CHUNK_SIZE_BYTES, properties);
        if (maximumChunkSizeBytesString != null) {
            try {
                int maximumChunkSizeBytesCandidate = Integer.parseInt(maximumChunkSizeBytesString);
                if ((maximumChunkSizeBytesCandidate < FIVE_KB) || (maximumChunkSizeBytesCandidate > ONE_HUNDRED_MB)) {
                    LOG.warning("Trying to set the chunk size to an unreasonable number. Using the default instead.");
                    maximumChunkSizeBytes = DEFAULT_MAXIMUM_CHUNK_SIZE_BYTES;
                } else {
                    maximumChunkSizeBytes = maximumChunkSizeBytesCandidate;
                }
            } catch (NumberFormatException e) {
                LOG.warning("Number format exception trying to set chunk size. Using the default instead.");
                maximumChunkSizeBytes = DEFAULT_MAXIMUM_CHUNK_SIZE_BYTES;
            }

        } else {
            maximumChunkSizeBytes = DEFAULT_MAXIMUM_CHUNK_SIZE_BYTES;
        }
        return maximumChunkSizeBytes;
    }


    /**
     * Extracts the value of bootstrapAsynchronously from the properties
     *
     * @param properties
     */
    protected boolean extractBootstrapAsynchronously(Properties properties) {
        boolean bootstrapAsynchronously;
        String bootstrapAsynchronouslyString = PropertyUtil.extractAndLogProperty(BOOTSTRAP_ASYNCHRONOUSLY, properties);
        if (bootstrapAsynchronouslyString != null) {
            bootstrapAsynchronously = PropertyUtil.parseBoolean(bootstrapAsynchronouslyString);
        } else {
            bootstrapAsynchronously = true;
        }
        return bootstrapAsynchronously;
    }
}

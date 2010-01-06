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

package net.sf.ehcache.loader;

import net.sf.ehcache.Ehcache;

import java.util.Properties;

/**
 * An abstract factory for creating cache writers. Implementers should provide their own
 * concrete factory extending this factory.
 * <p/>
 * Note that Ehcache API also allows the CacheLoader to be set programmatically.
 * @author Greg Luck
 * @version $Id$
 */
public abstract class CacheStorerFactory {

    /**
     * Creates a CacheLoader using the Ehcache configuration mechanism at the time the associated cache
     *  is created.
     * @param cache a reference to the owning cache
     * @param properties properties configured as delimiter separated name value pairs in ehcache.xml
     *
     * The following properties will be used:
     * <ul>
     * <li>writeBehind [true | false] - whether to run in write-behind or write-through mode. The default is write-through.
     * </ul>
     *
     * These properties apply to write-through mode only:
     * <ul>
     * notifyListenersOnException - whether to notify listeners when a an exception occurs on a storer operation. Defaults to false.
     *
     * <p/>
     * These properties apply to write-behind mode only:
     * <ul>
     * <li>maxWriteDelaySeconds        the maximum number of seconds to wait before writing behind. Defaults to 0. If set to a value
     *                                 greater than 0, it permits operations to build up in the queue to enable effective coalescing
     *                                 and batching optimisations.
     * <li>rateLimitPerSecond          the maximum number of store operations to allow per second. If writeBatching is enabled,
     * <li>writeCoalescing             whether to use write coalescing. Defaults to false. If set to true, if multiple operations
     *                                 on the same key are present in the write-behind queue, only the latest write is done, as the
     *                                 others are redundant. This can dramatically reduce load on the underlying resource.
     * <li>writeBatching               whether to batch write operations. Defaults to false. If set to true, storeAll and deleteAll
     *                                 will be called rather than store and delete being called for each key. Resources such as databases
     *                                 can perform more efficiently if updates are batched, thus reducing load.
     * <li>writeBatchSize              the number of operations to include in each batch. Defaults to 1. If there are less entries in the
     *                                 write-behind queue than the batch size, the queue length size is used.
     * <li>retryAttempts               the number of times to attempt. Defaults to 1.
     * <li>retryAttemptDelaySeconds    the number of seconds to wait before retrying.
     * </ul>
     *
     * The above properties should be passed through to the CacheStorer when this method creates it in the CacheStorer's Properties,
     * so that Ehcache can properly wire it in.
     *
     * Implementers may also configure additional properties which will be ignored by Ehcache, but may be useful for specifying
     * the underlying resource. e.g. dataSourceName could be specified and then looked up in JNDI.
     *
     * @return a constructed CacheLoader
     */
    public CacheStorer createCacheStorer(Ehcache cache, Properties properties) {

       //do set up required for ehcache.  

    }

    /**
     *
     * @param cache
     * @param properties
     * @return
     */
    protected abstract CacheStorer doCreateCacheStorer(Ehcache cache, Properties properties);
}
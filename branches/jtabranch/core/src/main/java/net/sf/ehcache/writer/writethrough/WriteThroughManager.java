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
package net.sf.ehcache.writer.writethrough;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterManager;
import net.sf.ehcache.writer.CacheWriterManagerException;

/**
 * Implements a {@code WriterManager} that writes elements directly through to the underlying store.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class WriteThroughManager implements CacheWriterManager {
    private volatile Cache cache;

    /**
     * {@inheritDoc}
     */
    public void init(Cache cache) throws CacheException {
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element) throws CacheException {
        try {
            CacheWriter writer = cache.getRegisteredCacheWriter();
            if (writer != null) {
                writer.write(element);
            }
        } catch (RuntimeException e) {
            throw new CacheWriterManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(Object key) throws CacheException {
        try {
            CacheWriter writer = cache.getRegisteredCacheWriter();
            if (writer != null) {
                writer.delete(key);
            }
        } catch (RuntimeException e) {
            throw new CacheWriterManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        // nothing to do
    }
}

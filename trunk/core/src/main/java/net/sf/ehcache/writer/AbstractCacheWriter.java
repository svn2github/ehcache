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
package net.sf.ehcache.writer;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.Collection;

/**
 * A convenience abstract base class that implements all {@code CacheWriter} methods.
 * <p/>
 * The {@link #write}, {@link #writeAll}, {@link #delete} and {@link #deleteAll} methods throw
 * {@code UnsupportedOperationException} unless they're overridden by the class that is extending
 * {@code AbstractCacheWriter}. Classes that are extending this abstract base class should make sure that the
 * appropriate cache writer operation methods are implemented with their application functionalities.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public abstract class AbstractCacheWriter implements CacheWriter {
    /**
     * {@inheritDoc}
     */
    public void write(Element element) throws CacheException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void writeAll(Collection<Element> elements) throws CacheException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void delete(CacheEntry entry) throws CacheException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAll(Collection<CacheEntry> entries) throws CacheException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public CacheWriter clone(Ehcache cache) throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    public void init() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() throws CacheException {
        // no-op
    }
}

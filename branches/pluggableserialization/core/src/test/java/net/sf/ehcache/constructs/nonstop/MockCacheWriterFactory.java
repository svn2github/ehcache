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

import java.util.Collection;
import java.util.Properties;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterFactory;

public class MockCacheWriterFactory extends CacheWriterFactory {

    @Override
    public CacheWriter createCacheWriter(Ehcache cache, Properties properties) {
        return new CacheWriter() {

            public void writeAll(Collection<Element> elements) throws CacheException {
                // no-op
            }

            public void write(Element element) throws CacheException {
                // no-op

            }

            public void init() {
                // no-op

            }

            public void dispose() throws CacheException {
                // no-op

            }

            public void deleteAll(Collection<CacheEntry> entries) throws CacheException {
                // no-op

            }

            public void delete(CacheEntry entry) throws CacheException {
                // no-op

            }

            public CacheWriter clone(Ehcache cache) throws CloneNotSupportedException {
                // no-op
                return null;
            }
        };
    }

}
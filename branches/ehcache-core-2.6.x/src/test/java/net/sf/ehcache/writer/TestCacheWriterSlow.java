/**
 *  Copyright Terracotta, Inc.
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

import java.util.Collection;
import java.util.Properties;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

public class TestCacheWriterSlow extends TestCacheWriter {

    public TestCacheWriterSlow() {
        super(new Properties());
    }

    @Override
    public synchronized void write(Element element) throws CacheException {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new CacheException(e);
        }
        super.write(element);
    }

    @Override
    public synchronized void writeAll(Collection<Element> elements) throws CacheException {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new CacheException(e);
        }
        super.writeAll(elements);
    }

    @Override
    public synchronized void delete(CacheEntry entry) throws CacheException {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new CacheException(e);
        }
        super.delete(entry);
    }

    @Override
    public synchronized void deleteAll(Collection<CacheEntry> entries) throws CacheException {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new CacheException(e);
        }
        super.deleteAll(entries);
    }
}

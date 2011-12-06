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

package net.sf.ehcache.store.compound.factories;

import junit.framework.Assert;

import org.junit.Test;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.DiskStore;
import net.sf.ehcache.store.Store;

import static org.junit.Assert.fail;

public class LegacyIndexFormatLoadTest {

    @Test
    public void testLegacyIndexFormatLoad() {
        DiskStoreConfiguration diskConfig = new DiskStoreConfiguration();
        diskConfig.setPath(System.getProperty("java.io.tmpdir"));
        Configuration config = new Configuration();
        config.addDefaultCache(new CacheConfiguration("test", 0));
        config.addDiskStore(diskConfig);

        CacheManager manager = new CacheManager(config);
        Cache persistent = new Cache(new CacheConfiguration("test", 0).diskPersistent(true));

        Store legacy = DiskStore.create(persistent, config.getDiskStoreConfiguration().getPath());

        for (int i = 0; i < 100; i++) {
            legacy.put(new Element(Integer.toString(i), "test"));
        }
        int millis = 500;
        while (legacy.getOnDiskSize() != 100) {
            try {
                Thread.sleep(millis);
                millis += millis;
                if (millis > 1500) {
                    fail("Looks like we don't get all entries on disk!");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        legacy.dispose();

        manager.addCache(persistent);

        Assert.assertEquals(100, persistent.getSize());

        for (Object key : persistent.getKeys()) {
            Assert.assertEquals("test", persistent.get(key).getObjectValue());
        }
    }
}

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

package net.sf.ehcache;

import java.io.InputStream;
import java.net.URL;

import junit.framework.TestCase;
import net.sf.ehcache.config.Configuration;

import org.junit.Assert;

public class SameCacheManagerNameTest extends TestCase {

    public void testSameCacheManager() {
        System.out.println("XXX Testing default constructor");
        CacheManager cacheManager = new CacheManager();
        try {
            new CacheManager();
            fail("Should have failed");
        } catch (CacheException e) {
            System.out.println("Caught expected exception: " + e);
            Assert.assertTrue(e.getMessage().contains(
                    "The source of the existing CacheManager is: DefaultConfigurationSource [ ehcache.xml or ehcache-failsafe.xml ]"));
            CacheManager alreadyCreatedCacheManager = CacheManager.create();
            Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
        } finally {
            cacheManager.shutdown();
            try {
                cacheManager = new CacheManager();
                CacheManager alreadyCreatedCacheManager = CacheManager.create();
                Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
            } catch (Throwable t) {
                fail("After shutdown should work - " + t);
            } finally {
                cacheManager.shutdown();
            }
        }

        System.out.println("XXX Testing programmatic configuration");
        Configuration config = new Configuration().name("some test name");
        cacheManager = new CacheManager(config);

        try {
            new CacheManager(config);
            fail("Should have failed");
        } catch (CacheException e) {
            System.out.println("Caught expected exception: " + e);
            Assert.assertTrue(e.getMessage().contains("The source of the existing CacheManager is: [Programmatically configured]"));
            CacheManager alreadyCreatedCacheManager = CacheManager.create(config);
            Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
        } finally {
            cacheManager.shutdown();
            try {
                cacheManager = new CacheManager(config);
                CacheManager alreadyCreatedCacheManager = CacheManager.create(config);
                Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
            } catch (Throwable t) {
                fail("After shutdown should work - " + t);
            } finally {
                cacheManager.shutdown();
            }
        }

        System.out.println("XXX Testing inputstream constructor");
        InputStream inputStream = CacheManager.class.getResourceAsStream("/ehcache-nodisk.xml");
        cacheManager = new CacheManager(inputStream);

        try {
            inputStream = CacheManager.class.getResourceAsStream("/ehcache-nodisk.xml");
            new CacheManager(inputStream);
            fail("Should have failed");
        } catch (CacheException e) {
            System.out.println("Caught expected exception: " + e);
            Assert.assertTrue(e.getMessage()
                    .contains("The source of the existing CacheManager is: InputStreamConfigurationSource [stream="));
            inputStream = CacheManager.class.getResourceAsStream("/ehcache-nodisk.xml");
            CacheManager alreadyCreatedCacheManager = CacheManager.create(inputStream);
            Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
        } finally {
            cacheManager.shutdown();
            try {
                inputStream = CacheManager.class.getResourceAsStream("/ehcache-nodisk.xml");
                cacheManager = new CacheManager(inputStream);
                inputStream = CacheManager.class.getResourceAsStream("/ehcache-nodisk.xml");
                CacheManager alreadyCreatedCacheManager = CacheManager.create(inputStream);
                Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
            } catch (Throwable t) {
                fail("After shutdown should work - " + t);
            } finally {
                cacheManager.shutdown();
            }
        }

        System.out.println("XXX Testing URL constructor");
        URL url = CacheManager.class.getResource("/ehcache-nodisk.xml");
        cacheManager = new CacheManager(url);

        try {
            cacheManager = new CacheManager(url);
            fail("Should have failed");
        } catch (CacheException e) {
            System.out.println("Caught expected exception: " + e);
            Assert.assertTrue(e.getMessage().contains("The source of the existing CacheManager is: URLConfigurationSource [url="));
            CacheManager alreadyCreatedCacheManager = CacheManager.create(url);
            Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
        } finally {
            cacheManager.shutdown();
            try {
                cacheManager = new CacheManager(url);
                CacheManager alreadyCreatedCacheManager = CacheManager.create(url);
                Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
            } catch (Throwable t) {
                fail("After shutdown should work - " + t);
            } finally {
                cacheManager.shutdown();
            }
        }

        System.out.println("XXX Testing fileName constructor");
        String configurationFileName = AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-nodisk.xml";
        cacheManager = new CacheManager(configurationFileName);

        try {
            new CacheManager(configurationFileName);
            fail("Should have failed");
        } catch (CacheException e) {
            System.out.println("Caught expected exception: " + e);
            Assert.assertTrue(e.getMessage().contains(
                    "The source of the existing CacheManager is: FileNameSource [file=src/test/resources/ehcache-nodisk.xml"));
            CacheManager alreadyCreatedCacheManager = CacheManager.create(configurationFileName);
            Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
        } finally {
            cacheManager.shutdown();
            try {
                cacheManager = new CacheManager(configurationFileName);
                CacheManager alreadyCreatedCacheManager = CacheManager.create(configurationFileName);
                Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
            } catch (Throwable t) {
                fail("After shutdown should work - " + t);
            } finally {
                cacheManager.shutdown();
            }
        }

    }

    public void testSameCacheManagersWithSingleton() {
        System.out.println("XXX Testing default constructor");
        CacheManager cacheManager = CacheManager.create();
        try {
            new CacheManager();
            fail("Should have failed");
        } catch (CacheException e) {
            System.out.println("Caught expected exception: " + e);
            Assert.assertTrue(e.getMessage().contains(
                    "The source of the existing CacheManager is: DefaultConfigurationSource [ ehcache.xml or ehcache-failsafe.xml ]"));
            CacheManager alreadyCreatedCacheManager = CacheManager.create();
            Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
        } finally {
            cacheManager.shutdown();
            try {
                cacheManager = new CacheManager();
                CacheManager alreadyCreatedCacheManager = CacheManager.create();
                Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
            } catch (Throwable t) {
                fail("After shutdown should work - " + t);
            } finally {
                cacheManager.shutdown();
            }
        }

        System.out.println("XXX Testing programmatic configuration");
        Configuration config = new Configuration().name("some test name");
        cacheManager = new CacheManager(config);

        try {
            new CacheManager(config);
            fail("Should have failed");
        } catch (CacheException e) {
            System.out.println("Caught expected exception: " + e);
            Assert.assertTrue(e.getMessage().contains("The source of the existing CacheManager is: [Programmatically configured]"));
            CacheManager alreadyCreatedCacheManager = CacheManager.create(config);
            Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
        } finally {
            cacheManager.shutdown();
            try {
                cacheManager = new CacheManager(config);
                CacheManager alreadyCreatedCacheManager = CacheManager.create(config);
                Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
            } catch (Throwable t) {
                fail("After shutdown should work - " + t);
            } finally {
                cacheManager.shutdown();
            }
        }

        System.out.println("XXX Testing inputstream constructor");
        InputStream inputStream = CacheManager.class.getResourceAsStream("/ehcache-nodisk.xml");
        cacheManager = new CacheManager(inputStream);

        try {
            inputStream = CacheManager.class.getResourceAsStream("/ehcache-nodisk.xml");
            new CacheManager(inputStream);
            fail("Should have failed");
        } catch (CacheException e) {
            System.out.println("Caught expected exception: " + e);
            Assert.assertTrue(e.getMessage()
                    .contains("The source of the existing CacheManager is: InputStreamConfigurationSource [stream="));
            inputStream = CacheManager.class.getResourceAsStream("/ehcache-nodisk.xml");
            CacheManager alreadyCreatedCacheManager = CacheManager.create(inputStream);
            Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
        } finally {
            cacheManager.shutdown();
            try {
                inputStream = CacheManager.class.getResourceAsStream("/ehcache-nodisk.xml");
                cacheManager = new CacheManager(inputStream);
                inputStream = CacheManager.class.getResourceAsStream("/ehcache-nodisk.xml");
                CacheManager alreadyCreatedCacheManager = CacheManager.create(inputStream);
                Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
            } catch (Throwable t) {
                fail("After shutdown should work - " + t);
            } finally {
                cacheManager.shutdown();
            }
        }

        System.out.println("XXX Testing URL constructor");
        URL url = CacheManager.class.getResource("/ehcache-nodisk.xml");
        cacheManager = new CacheManager(url);

        try {
            cacheManager = new CacheManager(url);
            fail("Should have failed");
        } catch (CacheException e) {
            System.out.println("Caught expected exception: " + e);
            Assert.assertTrue(e.getMessage().contains("The source of the existing CacheManager is: URLConfigurationSource [url="));
            CacheManager alreadyCreatedCacheManager = CacheManager.create(url);
            Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
        } finally {
            cacheManager.shutdown();
            try {
                cacheManager = new CacheManager(url);
                CacheManager alreadyCreatedCacheManager = CacheManager.create(url);
                Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
            } catch (Throwable t) {
                fail("After shutdown should work - " + t);
            } finally {
                cacheManager.shutdown();
            }
        }

        System.out.println("XXX Testing fileName constructor");
        String configurationFileName = AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-nodisk.xml";
        cacheManager = new CacheManager(configurationFileName);

        try {
            new CacheManager(configurationFileName);
            fail("Should have failed");
        } catch (CacheException e) {
            System.out.println("Caught expected exception: " + e);
            Assert.assertTrue(e.getMessage().contains(
                    "The source of the existing CacheManager is: FileNameSource [file=src/test/resources/ehcache-nodisk.xml"));
            CacheManager alreadyCreatedCacheManager = CacheManager.create(configurationFileName);
            Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
        } finally {
            cacheManager.shutdown();
            try {
                cacheManager = new CacheManager(configurationFileName);
                CacheManager alreadyCreatedCacheManager = CacheManager.create(configurationFileName);
                Assert.assertTrue(cacheManager == alreadyCreatedCacheManager);
            } catch (Throwable t) {
                fail("After shutdown should work - " + t);
            } finally {
                cacheManager.shutdown();
            }
        }
    }

}

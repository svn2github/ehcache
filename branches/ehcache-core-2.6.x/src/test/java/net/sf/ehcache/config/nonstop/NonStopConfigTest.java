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

package net.sf.ehcache.config.nonstop;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.ConfigError;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.generator.ConfigurationUtil;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.Collection;

public class NonStopConfigTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(NonStopConfigTest.class);

    public void testInvalidConfig() {
        try {
            CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/nonstop/nonstop-invalid-config-test.xml"));
            fail("Invalid config should have failed");
        } catch (CacheException e) {
            // make sure its the expected exception
            assertTrue(e.getMessage().contains("does not allow attribute \"one\""));
        }
    }

    public void testNonStopCacheConfig() {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/nonstop/nonstop-config-test.xml"));
        assertNonstopConfig(cacheManager.getCache("defaultConfig"), NonstopConfiguration.DEFAULT_ENABLED,
                NonstopConfiguration.DEFAULT_IMMEDIATE_TIMEOUT, NonstopConfiguration.DEFAULT_TIMEOUT_MILLIS,
                NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR.getType());

        assertNonstopConfig(cacheManager.getCache("one"), false, NonstopConfiguration.DEFAULT_IMMEDIATE_TIMEOUT,
                NonstopConfiguration.DEFAULT_TIMEOUT_MILLIS, NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR.getType());

        assertNonstopConfig(cacheManager.getCache("two"), false, false, NonstopConfiguration.DEFAULT_TIMEOUT_MILLIS,
                NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR.getType());
        assertNonstopConfig(cacheManager.getCache("three"), false, false, 12345, NonstopConfiguration.DEFAULT_TIMEOUT_BEHAVIOR
                .getType());
        assertNonstopConfig(cacheManager.getCache("four"), false, false, 12345, "localReads");
        cacheManager.shutdown();
    }

    private void assertNonstopConfig(Cache cache, boolean nonstop, boolean immediateTimeout, int timeoutMillis, String timeoutBehavior) {
        LOG.info("Checking for cache: " + cache.getName());
        CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
        assertNotNull(cacheConfiguration);
        TerracottaConfiguration terracottaConfiguration = cacheConfiguration.getTerracottaConfiguration();
        assertNotNull(terracottaConfiguration);
        NonstopConfiguration nonstopConfiguration = terracottaConfiguration.getNonstopConfiguration();
        assertNotNull(nonstopConfiguration);

        assertEquals(nonstop, nonstopConfiguration.isEnabled());
        assertEquals(immediateTimeout, nonstopConfiguration.isImmediateTimeout());
        assertEquals(timeoutMillis, nonstopConfiguration.getTimeoutMillis());
        assertEquals(timeoutBehavior, nonstopConfiguration.getTimeoutBehavior().getType());
    }


    public void testNonStopDefaultConfigWrites() {
        // DEV-8234
        NonstopConfiguration ns=new NonstopConfiguration().enabled(true);

        TerracottaConfiguration tcConf = new TerracottaConfiguration().clustered(true).coherent(true).consistency
                (TerracottaConfiguration.Consistency.STRONG);
        tcConf.getNonstopConfiguration().enabled(true).timeoutMillis(10000);

        TerracottaConfiguration tcConf2 = new TerracottaConfiguration().clustered(true).coherent(true).consistency
                (TerracottaConfiguration.Consistency.STRONG);
        tcConf2.getNonstopConfiguration().enabled(false).timeoutMillis(10000);

        // now, these two condifgs should be different, with only the first being enabled.

        Assert.assertTrue(tcConf.getNonstopConfiguration().isEnabled()!= tcConf2.getNonstopConfiguration()
                .isEnabled());

        // now, send it full trip
        CacheConfiguration cconf=new CacheConfiguration().name("foo").terracotta(tcConf);
        Configuration conf=new Configuration().cache(cconf);
        conf.terracotta(new TerracottaClientConfiguration().url("localhost","10000").rejoin(true));
        String asText= ConfigurationUtil.generateCacheManagerConfigurationText(conf);

        Assert.assertTrue(asText.contains("nonstop"));

        // finally parse it back in, make sure it all hangs together.
        Configuration parsedConfig = ConfigurationFactory
                .parseConfiguration(new BufferedInputStream(new ByteArrayInputStream(asText.getBytes())));

        Assert.assertTrue(parsedConfig.getCacheConfigurations().get("foo").getTerracottaConfiguration().getNonstopConfiguration().isEnabled());

        // no errors
        Collection<ConfigError> errors = parsedConfig.validate();
        Assert.assertEquals(errors.size(),0);

    }

}

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

package net.sf.ehcache.config;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.TerracottaConfiguration.CoherenceMode;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoherenceModeConfigTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(CoherenceModeConfigTest.class);

    @Test
    public void testCoherenceModeConfig() {
        CacheManager cacheManager = new CacheManager(this.getClass().getResourceAsStream("/ehcache-coherence-mode-test.xml"));
        Cache cache = cacheManager.getCache("defaultCoherenceMode");
        boolean coherent;
        CoherenceMode coherenceMode;
        coherent = cache.getCacheConfiguration().getTerracottaConfiguration().isCoherent();
        coherenceMode = cache.getCacheConfiguration().getTerracottaConfiguration().getCoherenceMode();
        LOG.info("Default coherent: " + coherent);
        LOG.info("Default Coherence mode: " + coherenceMode);
        assertEquals(true, coherent);
        assertEquals(CoherenceMode.STRICT, coherenceMode);

        cache = cacheManager.getCache("falseCoherenceMode");
        coherent = cache.getCacheConfiguration().getTerracottaConfiguration().isCoherent();
        coherenceMode = cache.getCacheConfiguration().getTerracottaConfiguration().getCoherenceMode();
        LOG.info("False coherent: " + coherent);
        LOG.info("False Coherence mode: " + coherenceMode);
        assertEquals(false, coherent);
        assertEquals(CoherenceMode.NON_STRICT, coherenceMode);

        cache = cacheManager.getCache("trueCoherenceMode");
        coherent = cache.getCacheConfiguration().getTerracottaConfiguration().isCoherent();
        coherenceMode = cache.getCacheConfiguration().getTerracottaConfiguration().getCoherenceMode();
        LOG.info("True coherent: " + coherent);
        LOG.info("True Coherence mode: " + coherenceMode);
        assertEquals(true, coherent);
        assertEquals(CoherenceMode.STRICT, coherenceMode);

        TerracottaConfiguration tcConfig = cache.getCacheConfiguration().getTerracottaConfiguration();
        tcConfig.setCoherent(false);
        assertEquals(CoherenceMode.NON_STRICT, cache.getCacheConfiguration().getTerracottaConfiguration().getCoherenceMode());

        tcConfig.setCoherent(true);
        assertEquals(CoherenceMode.STRICT, cache.getCacheConfiguration().getTerracottaConfiguration().getCoherenceMode());

        tcConfig.setCoherent("false");
        assertEquals(CoherenceMode.NON_STRICT, cache.getCacheConfiguration().getTerracottaConfiguration().getCoherenceMode());

        tcConfig.setCoherent("true");
        assertEquals(CoherenceMode.STRICT, cache.getCacheConfiguration().getTerracottaConfiguration().getCoherenceMode());

        tcConfig.setCoherenceMode(CoherenceMode.NON_STRICT);
        assertEquals(CoherenceMode.NON_STRICT, cache.getCacheConfiguration().getTerracottaConfiguration().getCoherenceMode());

        tcConfig.setCoherenceMode(CoherenceMode.STRICT);
        assertEquals(CoherenceMode.STRICT, cache.getCacheConfiguration().getTerracottaConfiguration().getCoherenceMode());
    }

}

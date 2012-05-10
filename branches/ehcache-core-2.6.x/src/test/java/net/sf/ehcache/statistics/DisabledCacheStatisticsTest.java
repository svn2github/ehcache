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

package net.sf.ehcache.statistics;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests that we pickup the cache statistics disabled setting from the config, and that we can enable stats programatically too.
 *
 * @author Chris Dennis
 */
public class DisabledCacheStatisticsTest extends AbstractCacheTest {

    /**
     * test enable disable
     */
    @Test
    public void testEnableDisable() {
        Cache cache = manager.getCache("disabledStats");

        // statistics are initially disabled
        assertFalse(cache.isStatisticsEnabled());
        assertFalse(cache.isSampledStatisticsEnabled());

        // enabling stats doesn't enable sampled stats
        cache.setStatisticsEnabled(true);
        assertTrue(cache.isStatisticsEnabled());
        assertFalse(cache.isSampledStatisticsEnabled());

        // can disable again
        cache.setStatisticsEnabled(false);
        assertFalse(cache.isStatisticsEnabled());
        assertFalse(cache.isSampledStatisticsEnabled());

        // enabling sampled enables both
        cache.setSampledStatisticsEnabled(true);
        assertTrue(cache.isStatisticsEnabled());
        assertTrue(cache.isSampledStatisticsEnabled());

        cache.setSampledStatisticsEnabled(false);
        assertTrue(cache.isStatisticsEnabled());
        assertFalse(cache.isSampledStatisticsEnabled());

        // enable sampled again
        cache.setSampledStatisticsEnabled(true);
        assertTrue(cache.isStatisticsEnabled());
        assertTrue(cache.isSampledStatisticsEnabled());

        // disabling live disables sampled too
        cache.setStatisticsEnabled(false);
        assertFalse(cache.isStatisticsEnabled());
        assertFalse(cache.isSampledStatisticsEnabled());
    }
}

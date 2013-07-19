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

package net.sf.ehcache.config;

import net.sf.ehcache.CacheManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.test.categories.CheckShorts;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Tests for CacheWriterConfiguration
 *
 * @version $Id $
 */
@Category(CheckShorts.class)
public class CacheWriterConfigurationTest {
    private static final Logger LOG = LoggerFactory.getLogger(CacheWriterConfigurationTest.class.getName());

    @Test
    public void testInstantiation() {
        CacheWriterConfiguration config = new CacheWriterConfiguration();
        assertEquals(CacheWriterConfiguration.DEFAULT_WRITE_MODE, config.getWriteMode());
        assertEquals(CacheWriterConfiguration.DEFAULT_NOTIFY_LISTENERS_ON_EXCEPTION, config.getNotifyListenersOnException());
        assertEquals(CacheWriterConfiguration.DEFAULT_MIN_WRITE_DELAY, config.getMinWriteDelay());
        assertEquals(CacheWriterConfiguration.DEFAULT_MAX_WRITE_DELAY, config.getMaxWriteDelay());
        assertEquals(CacheWriterConfiguration.DEFAULT_RATE_LIMIT_PER_SECOND, config.getRateLimitPerSecond());
        assertEquals(CacheWriterConfiguration.DEFAULT_WRITE_COALESCING, config.getWriteCoalescing());
        assertEquals(CacheWriterConfiguration.DEFAULT_WRITE_BATCHING, config.getWriteBatching());
        assertEquals(CacheWriterConfiguration.DEFAULT_WRITE_BATCH_SIZE, config.getWriteBatchSize());
        assertEquals(CacheWriterConfiguration.DEFAULT_RETRY_ATTEMPTS, config.getRetryAttempts());
        assertEquals(CacheWriterConfiguration.DEFAULT_RETRY_ATTEMPT_DELAY_SECONDS, config.getRetryAttemptDelaySeconds());
        assertEquals(CacheWriterConfiguration.DEFAULT_WRITE_BEHIND_CONCURRENCY, config.getWriteBehindConcurrency());
        assertEquals(CacheWriterConfiguration.DEFAULT_WRITE_BEHIND_MAX_QUEUE_SIZE, config.getWriteBehindMaxQueueSize());
    }

    @Test
    public void testZeros() {
        CacheWriterConfiguration config = new CacheWriterConfiguration()
                .minWriteDelay(0)
                .maxWriteDelay(0)
                .rateLimitPerSecond(0)
                .writeBatchSize(0)
                .retryAttempts(0)
                .retryAttemptDelaySeconds(0)
                .writeBehindConcurrency(0)
                .writeBehindMaxQueueSize(0);
        assertEquals(CacheWriterConfiguration.DEFAULT_WRITE_MODE, config.getWriteMode());
        assertEquals(0, config.getMinWriteDelay());
        assertEquals(0, config.getMaxWriteDelay());
        assertEquals(0, config.getRateLimitPerSecond());
        assertEquals(1, config.getWriteBatchSize());
        assertEquals(0, config.getRetryAttempts());
        assertEquals(0, config.getRetryAttemptDelaySeconds());
        assertEquals(1, config.getWriteBehindConcurrency());
        assertEquals(0, config.getWriteBehindMaxQueueSize());
    }

    @Test
    public void testInvalidValues() {
        CacheWriterConfiguration config = new CacheWriterConfiguration()
                .minWriteDelay(-20)
                .maxWriteDelay(-10)
                .rateLimitPerSecond(-30)
                .writeBatchSize(-40)
                .retryAttempts(-50)
                .retryAttemptDelaySeconds(-1)
                .writeBehindConcurrency(-345)
                .writeBehindMaxQueueSize(-1);
        assertEquals(CacheWriterConfiguration.DEFAULT_WRITE_MODE, config.getWriteMode());
        assertEquals(0, config.getMinWriteDelay());
        assertEquals(0, config.getMaxWriteDelay());
        assertEquals(0, config.getRateLimitPerSecond());
        assertEquals(1, config.getWriteBatchSize());
        assertEquals(0, config.getRetryAttempts());
        assertEquals(0, config.getRetryAttemptDelaySeconds());
        assertEquals(1, config.getWriteBehindConcurrency());
        assertEquals(0, config.getWriteBehindMaxQueueSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullWriteMode() {
        CacheWriterConfiguration config = new CacheWriterConfiguration()
                .writeMode((CacheWriterConfiguration.WriteMode) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidWriteMode() {
        CacheWriterConfiguration config = new CacheWriterConfiguration()
                .writeMode("invalid");
    }

    @Test
    public void testConfiguration() {
        CacheWriterConfiguration config = new CacheWriterConfiguration()
                .writeMode(CacheWriterConfiguration.WriteMode.WRITE_BEHIND)
                .notifyListenersOnException(true)
                .minWriteDelay(2)
                .maxWriteDelay(3)
                .rateLimitPerSecond(4)
                .writeCoalescing(true)
                .writeBatching(true)
                .writeBatchSize(5)
                .retryAttempts(6)
                .retryAttemptDelaySeconds(7)
                .writeBehindConcurrency(4)
                .writeBehindMaxQueueSize(125);
        assertEquals(CacheWriterConfiguration.WriteMode.WRITE_BEHIND, config.getWriteMode());
        assertEquals(true, config.getNotifyListenersOnException());
        assertEquals(2, config.getMinWriteDelay());
        assertEquals(3, config.getMaxWriteDelay());
        assertEquals(4, config.getRateLimitPerSecond());
        assertEquals(true, config.getWriteCoalescing());
        assertEquals(true, config.getWriteBatching());
        assertEquals(5, config.getWriteBatchSize());
        assertEquals(6, config.getRetryAttempts());
        assertEquals(7, config.getRetryAttemptDelaySeconds());
        assertEquals(4, config.getWriteBehindConcurrency());
        assertEquals(125, config.getWriteBehindMaxQueueSize());
    }


    @Test
    public void testBatchSizeWithoutBatchingFail() {
        CacheWriterConfiguration config=new CacheWriterConfiguration().writeBatching(false).writeBatchSize(10)
                .writeMode(CacheWriterConfiguration.WriteMode.WRITE_BEHIND);

        Collection<ConfigError> errs=new ArrayList<ConfigError>();
        config.validate(errs);
        Assert.assertFalse(errs.isEmpty());
        CacheConfiguration cacheConfig=new CacheConfiguration().name("foo").cacheWriter(config);
        try {
            cacheConfig.validateCompleteConfiguration();
            Assert.fail();
        } catch(InvalidConfigurationException ie) {
        }

        try {
            CacheManager cm = new CacheManager(getClass().getResourceAsStream
                    ("/ehcache-invalid-cachewriter-batching.xml"));
            Assert.fail("Should have failed with cache writer config error");
        } catch(InvalidConfigurationException ie) {
            // pass
        }

    }

}
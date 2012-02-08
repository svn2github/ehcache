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

package net.sf.ehcache.config.generator;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;

import org.junit.Test;

public class ConfigurationGeneratedParses {

    private static final String[] TEST_CONFIGS = new String[] {
        "ehcache.xml",
        "ehcache-big.xml",
        "ehcache-cacheextension.xml",
        "ehcache-comparator",
        "ehcache-copy",
        "ehcache-countinglisteners.xml",
        "ehcache-search.xml",
        "ehcache-tx-local.xml",
        "ehcache-tx-twopc.xml",
        "ehcache-writer.xml",
        "ehcacheUTF8.xml"
    };

    @Test
    public void testGeneratedConfigIsValid() throws CacheException, UnsupportedEncodingException {
        for (String config : TEST_CONFIGS) {
            CacheManager manager = new CacheManager(ConfigurationGeneratedParses.class.getResource(config));
            try {
                String generatedConfig = manager.getActiveConfigurationText();
                new CacheManager(new ByteArrayInputStream(generatedConfig.getBytes("UTF-8"))).shutdown();
            } finally {
                manager.shutdown();
            }
        }
    }
}

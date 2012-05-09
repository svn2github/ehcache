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

package net.sf.ehcache.terracotta;

import net.sf.ehcache.bootstrap.BootstrapCacheLoaderFactory;
import net.sf.ehcache.util.PropertyUtil;

import java.util.Properties;

/**
 * @author Alex Snaps
 */
public class TerracottaBootstrapCacheLoaderFactory extends BootstrapCacheLoaderFactory<TerracottaBootstrapCacheLoader> {

    @Override
    public TerracottaBootstrapCacheLoader createBootstrapCacheLoader(final Properties properties) {

        final boolean asynchronous = extractBootstrapAsynchronously(properties);
        TerracottaBootstrapCacheLoader cacheLoader;

        final String directory = PropertyUtil.extractAndLogProperty("directory", properties);
        if (extractBoolean(properties, "doKeySnapshot", true)) {
            cacheLoader = new TerracottaBootstrapCacheLoader(asynchronous,
                directory,
                extractLong(properties, "interval", TerracottaBootstrapCacheLoader.DEFAULT_INTERVAL),
                extractBoolean(properties, "useDedicatedThread", TerracottaBootstrapCacheLoader.DEFAULT_DEDICATED_THREAD)
            );
        } else {
            cacheLoader = new TerracottaBootstrapCacheLoader(asynchronous, directory, false);
        }
        cacheLoader.setImmediateShutdown(extractBoolean(properties, "immediateShutdown", true));
        cacheLoader.setSnapshotOnDispose(extractBoolean(properties, "doKeySnapshotOnDispose", false));
        return cacheLoader;

    }

}

/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.constructs.web.filter;

import net.sf.ehcache.CacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides a {@link net.sf.ehcache.constructs.blocking.BlockingCache} as a singleton.
 * for use by filters which are created on request.
 * @author Greg Luck
 * @version $Id$
 */
public final class FilterCacheManager {
    private static final Log LOG = LogFactory.getLog(FilterCacheManager.class.getName());

    private static CacheManager instance;


    private FilterCacheManager() {
        LOG.debug("Creating FilterCacheManager singleton instance");
    }

    /**
     * Gets a singleton instance of the {@link CacheManager} used by this filter
     */
    public static synchronized CacheManager getCacheManagerInstance() {
        if (instance == null) {
                instance = new CacheManager();
        }
        return instance;
    }

}

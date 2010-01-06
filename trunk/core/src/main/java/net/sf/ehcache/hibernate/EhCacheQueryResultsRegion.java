/**
 *  Copyright 2003-2009 Terracotta, Inc.
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
package net.sf.ehcache.hibernate;

import org.hibernate.cache.Cache;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.impl.bridge.BaseGeneralDataRegionAdapter;
import org.hibernate.cfg.Settings;

/**
 * A query results region specific wrapper around an EhCache instance.
 *
 * @author Chris Dennis
 */
class EhCacheQueryResultsRegion extends BaseGeneralDataRegionAdapter implements QueryResultsRegion {

    /**
     * Constructs an EhCacheQueryResultsRegion around the given underlying cache.
     */
    public EhCacheQueryResultsRegion(Cache underlyingCache, Settings settings) {
        super(underlyingCache, settings);
    }

}

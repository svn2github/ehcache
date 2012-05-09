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
package net.sf.ehcache.hibernate.regions;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.hibernate.strategy.EhcacheAccessStrategyFactory;

import org.hibernate.cache.QueryResultsRegion;

/**
 * A query results region specific wrapper around an Ehcache instance.
 *
 * @author Chris Dennis
 */
public class EhcacheQueryResultsRegion extends EhcacheGeneralDataRegion implements QueryResultsRegion {

    /**
     * Constructs an EhcacheQueryResultsRegion around the given underlying cache.
     * @param accessStrategyFactory
     */
    public EhcacheQueryResultsRegion(EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache underlyingCache, Properties properties) {
        super(accessStrategyFactory, underlyingCache, properties);
    }

}

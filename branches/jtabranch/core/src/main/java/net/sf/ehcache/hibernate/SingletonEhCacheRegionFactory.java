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

import java.util.Properties;

/**
 * A singleton EhCacheRegionFactory implementation.
 * <p>
 * This class uses functionality in the Hibernate 3.2 API SingletonEhCacheProvider.
 *
 * @author Chris Dennis
 */
public class SingletonEhCacheRegionFactory extends AbstractEhCacheRegionFactory {

    /**
     * Returns a representation of the singleton EhCacheRegionFactory
     */
    public SingletonEhCacheRegionFactory(Properties prop) {
        super(new SingletonEhCacheProvider());
    }
}

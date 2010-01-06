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
 * A non-singleton EhCacheRegionFactory implementation.
 * <p>
 * This class uses functionality in the Hibernate 3.2 API EhCacheProvider.
 * 
 * @author Chris Dennis
 */
public class EhCacheRegionFactory extends AbstractEhCacheRegionFactory {

    /**
     * Creates a non-singleton EhCacheRegionFactory
     */
    public EhCacheRegionFactory(Properties prop) {
        super(new EhCacheProvider());
    }
}

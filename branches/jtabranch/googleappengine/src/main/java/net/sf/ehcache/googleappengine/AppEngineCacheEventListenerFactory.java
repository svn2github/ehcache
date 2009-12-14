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



package net.sf.ehcache.googleappengine;

import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;

import java.util.Properties;

/**
 * @author C&eacute;drik LIME
 */
public class AppEngineCacheEventListenerFactory extends CacheEventListenerFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheEventListener createCacheEventListener(Properties properties) {
        return new AppEngineCacheEventListener(properties);
    }

}

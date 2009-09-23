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

package net.sf.ehcache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfigConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.StoreFactory;
import net.sf.ehcache.util.ClassLoaderUtil;

/**
 * A small helper class that knows how to create terracotta store factories
 *
 * @author teck
 */
class TerracottaStoreHelper {

    private static final String DIRECT_STORE_FACTORY = "org.terracotta.modules.ehcache.store.TerracottaStoreFactory";
    private static final String STANDALONE_STORE_FACTORY = "net.sf.ehcache.terracotta.StandaloneTerracottaStoreFactory";

    /**
     * Locate and decide which terracotta StoreFactory should be used. If the standalone factory class is available
     * it is preferred (ie. if ehcache-terracotta-xxx.jar is present)
     *
     * @param cacheConfigs
     * @return the selected terracotta store factory
     */
    static StoreFactory newStoreFactory(Map<String, CacheConfiguration> cacheConfigs, TerracottaConfigConfiguration terracottaConfig) {
        Class factoryClass;

        try {
            factoryClass = ClassLoaderUtil.loadClass(STANDALONE_STORE_FACTORY);

            // verify no identity caches if standalone will be used
            List<String> identityCaches = new ArrayList<String>();
            for (CacheConfiguration config : cacheConfigs.values()) {
                TerracottaConfiguration tcConfig = config.getTerracottaConfiguration();
                if (tcConfig != null && tcConfig.getValueMode() == TerracottaConfiguration.ValueMode.IDENTITY) {
                    identityCaches.add(config.getName());
                }
            }
            if (!identityCaches.isEmpty()) {
                throw new CacheException("One or more caches are configured for identity value " +
                                          "mode which is not permitted with standalone deployment " +
                                          identityCaches.toString());
            }
            
            // This is required in standalone but in non-standalone, this stuff is picked up through
            // the normal tc-config mechanisms instead
            if (terracottaConfig == null) {
                throw new CacheException(
                        "Terracotta caches are defined but no <terracottaConfig> element was used " +
                        "to specify the Terracotta configuration.");
            }
            
        } catch (ClassNotFoundException cnfe) {
            // assume not standalone usage if standalone factory not present
            try {
                factoryClass = ClassLoaderUtil.loadClass(DIRECT_STORE_FACTORY);
            } catch (ClassNotFoundException e) {
                // XXX: improve exception message here? A exception here can be caused by missing the TIM jar(s) in your app
                throw new CacheException("Terracotta cache classes are not available, you are missing jar(s) most likely", e);
            }
            
            if (terracottaConfig != null) {
                throw new CacheException("The ehcache configuration specified Terracotta configuration information, " +
                        "but when using the full install of Terracotta, you must specify the Terracotta configuration " +
                        "only with an external tc-config.xml file, not embedded or referenced from the ehcache " + 
                        "configuration file.");
            }
        }

        return (StoreFactory) ClassLoaderUtil.createNewInstance(factoryClass.getName(), 
                new Class[] {TerracottaConfigConfiguration.class}, 
                new Object[] {terracottaConfig});
    }

}

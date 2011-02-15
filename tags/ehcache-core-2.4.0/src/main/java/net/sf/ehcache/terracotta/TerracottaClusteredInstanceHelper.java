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

package net.sf.ehcache.terracotta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.util.ClassLoaderUtil;

/**
 * A small helper class that knows how to create terracotta store factories
 *
 * @author teck
 * @author Abhishek Sanoujam
 */
class TerracottaClusteredInstanceHelper {

    /**
     * Singleton instance
     */
    private static TerracottaClusteredInstanceHelper instance = new TerracottaClusteredInstanceHelper();

    /**
     * Private constructor
     */
    private TerracottaClusteredInstanceHelper() {
        lookupTerracottaRuntime();
    }

    /**
     * Returns the singleton instance
     *
     * @return
     */
    public static TerracottaClusteredInstanceHelper getInstance() {
        return instance;
    }

    /*
     * THIS METHOD IS NOT FOR PUBLIC USE
     * Used in tests only via reflection
     *
     * @param testHelper
     */
    private static void setTestMode(TerracottaClusteredInstanceHelper testHelper) {
        instance = testHelper;
    }

    /**
     * Enum for type of Terracotta runtime
     */
    static enum TerracottaRuntimeType {
        /**
         * Enum representing Enterprise Express mode
         */
        EnterpriseExpress(ENTERPRISE_EXPRESS_FACTORY),
        /**
         * Enum representing Express mode
         */
        Express(EXPRESS_FACTORY),
        /**
         * Enum representing Enterprise Custom mode
         */
        EnterpriseCustom(ENTERPRISE_CUSTOM_FACTORY),
        /**
         * Enum representing Custom mode
         */
        Custom(CUSTOM_FACTORY);

        private final String factoryClassName;

        /**
         * Private constructor
         *
         * @param factoryClassName
         */
        private TerracottaRuntimeType(String factoryClassName) {
            this.factoryClassName = factoryClassName;
        }

        /**
         * Returns the factory class for this mode or null if class is not present in classpath
         *
         * @return the factory class for this mode or null if class is not present in classpath
         */
        public Class getFactoryClassOrNull() {
            try {
                return ClassLoaderUtil.loadClass(factoryClassName);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

    /**
     * Boolean indicating if TC is running or not.
     * Can be stored in a static final field as required only in DSO mode.
     */
    private static final boolean TC_DSO_MODE = Boolean.getBoolean("tc.active");

    private static final String ENTERPRISE_EXPRESS_FACTORY =
        "net.sf.ehcache.terracotta.ExpressEnterpriseTerracottaClusteredInstanceFactory";
    private static final String ENTERPRISE_CUSTOM_FACTORY =
        "org.terracotta.modules.ehcache.store.EnterpriseTerracottaClusteredInstanceFactory";
    private static final String EXPRESS_FACTORY = "net.sf.ehcache.terracotta.StandaloneTerracottaClusteredInstanceFactory";
    private static final String CUSTOM_FACTORY = "org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory";

    private volatile TerracottaRuntimeType terracottaRuntimeType;

    /**
     * Lookup the current terracotta runtime
     *
     * @return the current terracotta runtime
     */
    private TerracottaRuntimeType lookupTerracottaRuntime() {
        if (terracottaRuntimeType == null) {
            final TerracottaRuntimeType[] lookupSequence = {TerracottaRuntimeType.EnterpriseExpress,
                    TerracottaRuntimeType.EnterpriseCustom, TerracottaRuntimeType.Express, TerracottaRuntimeType.Custom, };
            for (TerracottaRuntimeType type : lookupSequence) {
                if (type.getFactoryClassOrNull() != null) {
                    terracottaRuntimeType = type;
                    break;
                }
            }
        }
        return terracottaRuntimeType;
    }

    /**
     * Locate and decide which terracotta ClusteredInstanceFactory should be used. If the standalone factory class is available
     * it is preferred (ie. if ehcache-terracotta-xxx.jar is present)
     *
     * @param cacheConfigs
     * @return the selected terracotta store factory
     */
    ClusteredInstanceFactory newClusteredInstanceFactory(Map<String, CacheConfiguration> cacheConfigs,
            TerracottaClientConfiguration terracottaConfig) {
        lookupTerracottaRuntime();
        if (terracottaRuntimeType == null) {
            throw new CacheException("Terracotta cache classes are not available, you are missing jar(s) most likely");
        }

        if (terracottaRuntimeType == TerracottaRuntimeType.EnterpriseExpress || terracottaRuntimeType == TerracottaRuntimeType.Express) {
            assertExpress(cacheConfigs, terracottaConfig);
        } else if (terracottaRuntimeType == TerracottaRuntimeType.EnterpriseCustom
                || terracottaRuntimeType == TerracottaRuntimeType.Custom) {
            assertCustom(terracottaConfig);
        } else {
            throw new CacheException("Unknown Terracotta runtime type - " + terracottaRuntimeType);
        }

        Class factoryClass = terracottaRuntimeType.getFactoryClassOrNull();
        if (factoryClass == null) {
            throw new CacheException("Not able to get factory class for: " + terracottaRuntimeType.name());
        }
        return (ClusteredInstanceFactory) ClassLoaderUtil.createNewInstance(factoryClass.getName(),
                new Class[] {TerracottaClientConfiguration.class }, new Object[] {terracottaConfig });
    }

    private static void assertCustom(TerracottaClientConfiguration terracottaConfig) {
        if (!TC_DSO_MODE) {
            // required tim jars found in classpath but tc is not running.
            throw new CacheException("When not using standalone deployment, you need to use full install of Terracotta"
                    + " in order to use Terracotta Clustered Caches.");
        }

        if (terracottaConfig != null) {
            throw new CacheException("The ehcache configuration specified Terracotta configuration information, "
                    + "but when using the full install of Terracotta, you must specify the Terracotta configuration "
                    + "only with an external tc-config.xml file, not embedded or referenced from the ehcache " + "configuration file.");
        }
    }

    private static void assertExpress(Map<String, CacheConfiguration> cacheConfigs, TerracottaClientConfiguration terracottaConfig) {
        // verify no identity caches if standalone will be used
        List<String> identityCaches = new ArrayList<String>();
        for (CacheConfiguration config : cacheConfigs.values()) {
            TerracottaConfiguration tcConfig = config.getTerracottaConfiguration();
            if (tcConfig != null && tcConfig.getValueMode() == TerracottaConfiguration.ValueMode.IDENTITY) {
                identityCaches.add(config.getName());
            }
        }
        if (!identityCaches.isEmpty()) {
            throw newExceptionIdentityNotSupportedInExpress(identityCaches);
        }

        // This is required in standalone but in non-standalone, this stuff is picked up through
        // the normal tc-config mechanisms instead
        if (terracottaConfig == null) {
            throw new CacheException("Terracotta caches are defined but no <terracottaConfig> element was used "
                    + "to specify the Terracotta configuration.");
        }
    }

    private static CacheException newExceptionIdentityNotSupportedInExpress(List<String> identityCaches) {
        return new CacheException("One or more caches are configured for identity value "
                + "mode which is not permitted with standalone deployment " + identityCaches.toString());
    }

    /**
     * Returns the default {@link StorageStrategy} type for the current Terracotta runtime.
     * @param cacheConfiguration the cache configuration
     *
     * @return the default {@link StorageStrategy} type for the current Terracotta runtime.
     */
    StorageStrategy getDefaultStorageStrategyForCurrentRuntime(CacheConfiguration cacheConfiguration) {
        lookupTerracottaRuntime();
        if (terracottaRuntimeType == null) {
            throw new CacheException("Terracotta cache classes are not available, you are missing jar(s) most likely");
        }
        switch (cacheConfiguration.getTerracottaConfiguration().getValueMode()) {
            case SERIALIZATION:
                // default dcv2 for all 4 terracotta runtime types
                switch (terracottaRuntimeType) {
                    case Express:
                    case Custom:
                    case EnterpriseCustom:
                    case EnterpriseExpress:
                        return StorageStrategy.DCV2;
                    default:
                        throw new CacheException("Unknown Terracotta runtime type - " + terracottaRuntimeType);
                }
            case IDENTITY:
                switch (terracottaRuntimeType) {
                    case Custom:
                    case EnterpriseCustom:
                        return StorageStrategy.CLASSIC;
                    case Express:
                    case EnterpriseExpress:
                        throw newExceptionIdentityNotSupportedInExpress(Collections.singletonList(cacheConfiguration.getName()));
                    default:
                        throw new CacheException("Unknown Terracotta runtime type - " + terracottaRuntimeType);
                }
            default:
                throw new CacheException("Unknown value mode - " + cacheConfiguration.getTerracottaConfiguration().getValueMode());
        }
    }

    /**
     * Returns the terracotta runtime type or null if no runtime could be found
     *
     * @return the terracotta runtime type or null if no runtime could be found
     */
    TerracottaRuntimeType getTerracottaRuntimeTypeOrNull() {
        return terracottaRuntimeType;
    }

}

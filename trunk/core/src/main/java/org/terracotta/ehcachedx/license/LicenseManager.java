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

package org.terracotta.ehcachedx.license;

import net.sf.ehcache.util.MemorySizeParser;

import org.terracotta.ehcachedx.license.util.AbstractLicenseResolverFactory;

/**
 * Manager to handle all license check of enterprise features
 * 
 * @author hhuynh
 * 
 */
public class LicenseManager {
    private static volatile boolean hasInit;

    // lazily-init, don't use directly
    // use getLicense() instead
    private static License license;

    private static synchronized void init() {
        license = AbstractLicenseResolverFactory.getFactory().resolveLicense();
        hasInit = true;
    }

    private static synchronized License getLicense() {
        if (!hasInit) {
            init();
        }
        return license;
    }

    /**
     * Check if users has license to use offheap
     * @param maxHeap offheapMaxSize specified by cache configuration
     */
    public static void verifyOffHeapUsage(String maxHeap) {
        assertLicenseExists();
        if (!getLicense().isEnterpriseFeatureEnabled(LicenseConstants.EHCACHE_FEATURE_OFFHEAP)) {
            throw new LicenseException("Your license key doesn't allow usage of OffHeap feature");
        }
        String maxHeapSizeFromLicense = getLicense().getRequiredProperty(LicenseConstants.SETTING_EHCACHE_OFFHEAP_MAX_HEAP_SIZE);
        long maxHeapAllowedInBytes = MemorySizeParser.parse(maxHeapSizeFromLicense);
        long askingMaxHeap = MemorySizeParser.parse(maxHeap);
        if (askingMaxHeap > maxHeapAllowedInBytes) {
            throw new LicenseException("offheap max size set at " + maxHeap + " but your license only allows up to "
                    + maxHeapSizeFromLicense + ".");
        }
    }
    
    /**
     * This function should be called before accessing a Enterprise only feature
     * to check for the existence of a licence key
     * LicenceseException will be thrown if no key was found
     */
    public static void assertLicenseExists() {
        if (getLicense() == null) {
            throw new LicenseException("Access to Enterprise feature required Enterprise edition and a license key");
        }
    }
}

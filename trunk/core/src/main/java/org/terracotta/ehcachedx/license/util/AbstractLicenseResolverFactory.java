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
package org.terracotta.ehcachedx.license.util;

import org.terracotta.ehcachedx.license.License;

/**
 * 
 * @author hhuynh
 * 
 */
public abstract class AbstractLicenseResolverFactory extends AbstractFactory {
    private static final String FACTORY_SERVICE_ID = "org.terracotta.ehcachedx.LicenseResolver";
    private static final Class OPENSOURCE_RESOLVER_FACTORY_CLASS = OpensourceLicenseResolverFactory.class;

    /**
     * Either load the enterprise or opensource license factory at runtime
     * 
     * @return
     */
    public static AbstractLicenseResolverFactory getFactory() {
        return (AbstractLicenseResolverFactory) getFactory(FACTORY_SERVICE_ID, OPENSOURCE_RESOLVER_FACTORY_CLASS);
    }

    /**
     * Resolves the Terracotta license key via various settings
     * 
     * @return license instance
     */
    public abstract License resolveLicense();
}
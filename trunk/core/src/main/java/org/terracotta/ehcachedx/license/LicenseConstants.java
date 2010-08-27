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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author hhuynh
 *
 */
public class LicenseConstants {
    public static final String LICENSE_TYPE = "License.type";
    public static final String LICENSE_NUMBER = "License.number";
    public static final String LICENSE_LICENSEE = "Licensee";
    public static final String LICENSE_EXPIRATION_DATE = "Expiration.date";
    public static final String LICENSE_MAX_CLIENTS = "Max.clients";
    public static final String LICENSE_PRODUCT = "Product";
    public static final String LICENSE_EDITION = "Edition";
    public static final String LICENSE_CAPABILITIES = "Capabilities";
    public static final String LICENSE_SIGNATURE = "Signature";

    public static final String CAPABILITY_ROOTS = "roots";
    public static final String CAPABILITY_SESSIONS = "sessions";
    public static final String CAPABILITY_TOC = "Terracotta operator console";
    public static final String CAPABILITY_SERVER_STRIPING = "server striping";
    public static final String CAPABILITY_DCV2 = "DCV2";
    public static final String CAPABILITY_AUTHENTICATION = "authentication";
    public static final String CAPABILITY_EHCACHE = "ehcache";
    public static final String CAPABILITY_QUARTZ = "quartz";

    public static final String LICENSE_TYPE_COMMERCIAL = "Commercial";
    public static final String LICENSE_TYPE_TRIAL = "Trial";
    public static final String LICENSE_TYPE_OPENSOURCE = "Opensource";

    public static final String EDITION_ES = "ES";
    public static final String EDITION_EX = "EX";
    public static final String EDITION_FX = "FX";
    public static final String EDITION_DX = "DX";
    public static final String EDITION_CUSTOM = "Custom";

    public static final String LICENSE_CANONICAL_ENCODING = "UTF-8";
    public static final String LICENSE_DATE_FORMAT = "yyyy-MM-dd";
    public static final String LICENSE_KEY_FILENAME = "terracotta-license.key";

    public static final String PRODUCT_ENTERPRISE_SUITE = "Enterprise Suite";
    public static final String PRODUCT_EHCACHE = "Ehcache";
    public static final String PRODUCT_QUARTZ = "Quartz";
    public static final String PRODUCT_CUSTOM = "Custom";
    public static final String PRODUCT_SESSIONS = "Sessions";

    public static final String EHCACHE_FEATURES = "Ehcache.features";
    public static final String EHCACHE_FEATURE_MONITOR = "monitor";
    public static final String EHCACHE_FEATURE_OFFHEAP = "offheap";

    public static final String SETTING_PREFIX = "setting.";
    public static final String SETTING_EHCACHE_OFFHEAP_MAX_HEAP_SIZE = SETTING_PREFIX + "ehcache.offheap.max-heap-size";

    public static final List<String> IGNORED_KEYS = Arrays.asList(LICENSE_SIGNATURE);

    /**
     * @return the new instance date formatter with predefined format
     */
    public static SimpleDateFormat dateFormatter() {
        return new SimpleDateFormat(LICENSE_DATE_FORMAT);
    }
}

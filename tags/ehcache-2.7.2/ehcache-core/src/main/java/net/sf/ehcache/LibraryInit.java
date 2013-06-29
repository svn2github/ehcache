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

package net.sf.ehcache;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Static library initialization
 *
 * @author teck
 */
final class LibraryInit {

    private static final CacheException NO_ERROR = new CacheException();

    private static CacheException initError = null;

    private LibraryInit() {
        //
    }

    /**
     * Init the ehcache library
     */
    static synchronized void init() {
        if (initError != null) {
            if (initError == NO_ERROR) {
                return;
            }
            throw initError;
        }

        try {
            initService();
        } catch (Throwable t) {
            if (t instanceof CacheException) {
                initError = (CacheException) t;
            } else {
                initError = new CacheException(t);
            }
            throw initError;
        }

        initError = NO_ERROR;
    }

    private static void initService() {
        List<EhcacheInit> initializers = new ArrayList<EhcacheInit>();

        // prefer the TCCL based lookup, fallback to ehcache loader only if empty
        for (EhcacheInit init : ServiceLoader.load(EhcacheInit.class)) {
            initializers.add(init);
        }

        if (initializers.isEmpty()) {
            for (EhcacheInit init : ServiceLoader.load(EhcacheInit.class, CacheManager.class.getClassLoader())) {
                initializers.add(init);
            }
        }

        if (initializers.isEmpty()) {
            throw new AssertionError("No " + EhcacheInit.class.getName() + " services found");
        }

        for (EhcacheInit init : initializers) {
            init.init();
        }
    }
}

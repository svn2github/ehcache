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

package net.sf.ehcache.search.query;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A {@link net.sf.ehcache.search.query.QueryManager Query Manager} builder providing methods to add caches which
 * can be queried.
 * The Query Manager instance returned by this builder can be used to execute
 * search queries expressed as Big Memory Structured Query Language (BMSQL) statements.
 *
 * @author dkumar
 */
public final class QueryManagerBuilder {

    private final Collection<Ehcache> caches = new ArrayList<Ehcache>();
    private final Class<? extends QueryManager> defaultClass;


    private QueryManagerBuilder() {
        this(getImplementationClass());
    }

    /**
     * Package-local constructor for testing purposes.
     *
     * @param implementationClass a concrete implementation of the {@link net.sf.ehcache.search.query.QueryManager Query Manager} interface
     */
    QueryManagerBuilder(Class<? extends QueryManager> implementationClass) {
        this.defaultClass = implementationClass;
    }

    private static Class<? extends QueryManager> getImplementationClass() {
        try {
            return (Class<? extends QueryManager>)Class.forName("net.sf.ehcache.search.parser.QueryManagerImpl");
        } catch (ClassNotFoundException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Creates a new {@link net.sf.ehcache.search.query.QueryManager Query Manager} builder.
     *
     * @return this for the builder pattern
     */
    public static QueryManagerBuilder newQueryManagerBuilder() {
        return new QueryManagerBuilder();
    }

    /**
     * Adds a {@link net.sf.ehcache.Ehcache Cache} to the internal state and allows it to be used as a target in a
     * BMSQL statement's FROM clause.
     *
     * @param cache a {@link net.sf.ehcache.Ehcache cache} instance to be added
     * @return this for the builder pattern
     */
    public QueryManagerBuilder addCache(Ehcache cache) {
        this.caches.add(cache);
        return this;
    }

    /**
     * Adds all {@link net.sf.ehcache.Ehcache Caches} present in a {@link net.sf.ehcache.CacheManager Cache Manager} to
     * the internal state of the query manager.
     * The added caches can be used in a BMSQL statement's FROM clause.
     *
     * @param cacheManager a {@link net.sf.ehcache.CacheManager Cache Manager} whose
     *                     {@link net.sf.ehcache.Ehcache caches} need to be added
     * @return this for the builder pattern
     */
    public QueryManagerBuilder addAllCachesCurrentlyIn(CacheManager cacheManager) {
        for (String s : cacheManager.getCacheNames()) {
            final Ehcache cache = cacheManager.getEhcache(s);
            if (cache != null) {
                this.caches.add(cache);
            }
        }
        return this;
    }

    /**
     * Returns a new {@link net.sf.ehcache.search.query.QueryManager Query Manager} using the options supplied during the building
     * process.
     *
     * @return the fully constructed {@link net.sf.ehcache.search.query.QueryManager Query Manager}
     */
    public QueryManager build() {
        try {
            final Constructor<? extends QueryManager> constructor = defaultClass.getConstructor(Collection.class);
            return constructor.newInstance(this.caches);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

}

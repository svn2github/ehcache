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
import net.sf.ehcache.search.Query;

/**
 * Main entry point from Ehcache into the query manager responsible
 * for parsing SQL-like statements and returning a net.sf.ehcache.search.Query object.
 * <p/>
 * This interface obviates Ehcache's dependence on a specific dialect of SQL.
 * Implementations are free to decide which SQL-like language is supported by them.
 *
 * @author dkumar
 */
public interface QueryManager {

    /**
     * Parses a {@link java.lang.String String} statement expressing an Ehcache Search query and returns
     * a {@link net.sf.ehcache.search.Query Query} object for the cache specified in the statement.
     *
     * @param statement a String expressing an Ehcache Search query
     * @return a {@link net.sf.ehcache.search.Query Query}object tied to the cache specified in the statement
     * @throws CacheException if the cache could not be found or if a parse error occurs
     */
    Query createQuery(String statement) throws CacheException;
}

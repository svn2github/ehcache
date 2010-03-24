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

package net.sf.ehcache.transaction;

import net.sf.ehcache.store.Store;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * A Command represents an operation to be executed on a {@link Store}.
 * 
 * @author Nabib El-Rahman
 * @author Alex Snaps
 *
 */
public interface Command {

    /**
     * No op command
     */
    public static final String NULL = "NULL";

    /**
     * {@link Store#put(net.sf.ehcache.Element)} command
     */
    public static final String PUT_WITH_WRITER = "PUT_WITH_WRITER";

    /**
     * {@link Store#putWithWriter(net.sf.ehcache.Element, net.sf.ehcache.writer.CacheWriterManager)} command
     */
    public static final String PUT = "PUT";

    /**
     * {@link Store#remove(Object)} command
     */
    public static final String REMOVE = "REMOVE";

    /**
     * {@link Store#removeWithWriter(Object, net.sf.ehcache.writer.CacheWriterManager)} command
     */
    public static final String REMOVE_WITH_WRITER = "REMOVE_WITH_WRITER";

    /**
     * {@link net.sf.ehcache.store.Store#expireElements()} command
     */
    public static final String EXPIRE_ALL_ELEMENTS = "EXPIRE_ALL_ELEMENTS";

    /**
     * {@link net.sf.ehcache.store.Store#removeAll()}
     */
    public static final String REMOVE_ALL = "REMOVE_ALL";

    /**
     *
     * @return the command name
     */
    public String getCommandName();

    /**
     * Executes the command on some store
     * @param store the Store on which to execute the command
     * @return true if the store was mutated, false otherwise
     */
    boolean execute(Store store);

    /**
     * Executes the command on some cacheWriterManager 
     * @param cacheWriterManager the CacheWriterManager on which to execute the command
     * @return true if the CacheWriterManager was called
     */
    boolean execute(CacheWriterManager cacheWriterManager);

    /**
     * Is this command represents adding a key to the store
     * @param key the key
     * @return true, if this command would try to add an Element for key, otherwise false
     */
    public boolean isPut(Object key);

    /**
     * Is this command represents removing a key to the store
     * @param key the key
     * @return true, if this command would try to remove an Element for key, otherwise false
     */
    public boolean isRemove(Object key);

}

package net.sf.ehcache.store;

import net.sf.ehcache.Cache;

/**
 * Factory for creating Store implementations
 *
 * @author teck
 * @since 1.7
 */
public interface StoreFactory {

    Store create(Cache cache);

}

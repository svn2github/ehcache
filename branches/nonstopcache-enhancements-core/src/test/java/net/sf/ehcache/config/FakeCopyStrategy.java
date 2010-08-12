package net.sf.ehcache.config;

import net.sf.ehcache.store.compound.CopyStrategy;

/**
 * @author Alex Snaps
 */
public class FakeCopyStrategy implements CopyStrategy {

    public <T> T copy(final T value) {
        return null;
    }
}

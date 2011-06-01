package net.sf.ehcache.pool.impl;

import java.io.IOException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.pool.SizeOfEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.sizeof.AgentSizeOf;
import org.terracotta.modules.sizeof.ReflectionSizeOf;
import org.terracotta.modules.sizeof.SizeOf;
import org.terracotta.modules.sizeof.UnsafeSizeOf;
import org.terracotta.modules.sizeof.filter.AnnotationSizeOfFilter;
import org.terracotta.modules.sizeof.filter.CombinationSizeOfFilter;
import org.terracotta.modules.sizeof.filter.SizeOfFilter;
import org.terracotta.modules.sizeof.filter.ResourceSizeOfFilter;

/**
 * @author Alex Snaps
 */
public class DefaultSizeOfEngine implements SizeOfEngine {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSizeOfEngine.class.getName());

    private static final SizeOfFilter DEFAULT_FILTER;
    static {
        SizeOfFilter filter;
        try {
            filter = new CombinationSizeOfFilter(
                    new AnnotationSizeOfFilter(),
                    new ResourceSizeOfFilter(SizeOfEngine.class.getResource("hibernate.sizeof.filter")));
        } catch (IOException e) {
            filter = new AnnotationSizeOfFilter();
        }
        DEFAULT_FILTER = filter;
    }

    private final SizeOf sizeOf;

    public DefaultSizeOfEngine() {
        SizeOf sizeOf;
        try {
            sizeOf = new AgentSizeOf(DEFAULT_FILTER);
        } catch (UnsupportedOperationException e) {
            try {
                sizeOf = new UnsafeSizeOf(DEFAULT_FILTER);
            } catch (UnsupportedOperationException f) {
                try {
                    sizeOf = new ReflectionSizeOf(DEFAULT_FILTER);
                } catch (UnsupportedOperationException g) {
                    throw new CacheException("A suitable SizeOf engine could not be loaded: " + e + ", " + f + ", " + g);
                }
            }
        }

        this.sizeOf = sizeOf;
    }

    public long sizeOf(final Object key, final Object value, final Object container) {
        long size = sizeOf.deepSizeOf(key, value, container);
        LOG.debug("size of {}/{}/{} -> {}", new Object[]{key, value, container, size});
        return size;
    }
}

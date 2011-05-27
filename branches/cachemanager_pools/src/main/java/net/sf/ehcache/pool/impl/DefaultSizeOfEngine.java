package net.sf.ehcache.pool.impl;

import java.io.IOException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.pool.SizeOfEngine;

import org.terracotta.modules.sizeof.AgentSizeOf;
import org.terracotta.modules.sizeof.ReflectionSizeOf;
import org.terracotta.modules.sizeof.SizeOf;
import org.terracotta.modules.sizeof.UnsafeSizeOf;
import org.terracotta.modules.sizeof.filter.AnnotationFieldFilter;
import org.terracotta.modules.sizeof.filter.CombinationFieldFilter;
import org.terracotta.modules.sizeof.filter.FieldFilter;
import org.terracotta.modules.sizeof.filter.ResourceFieldFilter;

/**
 * @author Alex Snaps
 */
public class DefaultSizeOfEngine implements SizeOfEngine {

    private static final FieldFilter DEFAULT_FIELD_FILTER;
    static {
        FieldFilter filter;
        try {
            filter = new CombinationFieldFilter(
                    new AnnotationFieldFilter(),
                    new ResourceFieldFilter(SizeOfEngine.class.getResource("hibernate.sizeof.filter")));
        } catch (IOException e) {
            filter = new AnnotationFieldFilter();
        }
        DEFAULT_FIELD_FILTER = filter;
    }

    private final SizeOf sizeOf;

    public DefaultSizeOfEngine() {
        SizeOf sizeOf;
        try {
            sizeOf = new AgentSizeOf(DEFAULT_FIELD_FILTER);
        } catch (UnsupportedOperationException e) {
            try {
                sizeOf = new UnsafeSizeOf(DEFAULT_FIELD_FILTER);
            } catch (UnsupportedOperationException f) {
                try {
                    sizeOf = new ReflectionSizeOf(DEFAULT_FIELD_FILTER);
                } catch (UnsupportedOperationException g) {
                    throw new CacheException("A suitable SizeOf engine could not be loaded: " + e + ", " + f + ", " + g);
                }
            }
        }

        this.sizeOf = sizeOf;
    }

    public long sizeOf(final Object key, final Object value, final Object container) {
        return sizeOf.deepSizeOf(key, value, container);
    }
}

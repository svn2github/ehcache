package net.sf.ehcache.pool.impl;

import java.io.IOException;

import net.sf.ehcache.pool.SizeOfEngine;
import org.terracotta.modules.sizeof.SizeOf;
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

    private final SizeOf sizeOf = new SizeOf(DEFAULT_FIELD_FILTER);

    public long sizeOf(final Object key, final Object value, final Object container) {
        return sizeOf.deepSizeOf(key, value, container);
    }
}

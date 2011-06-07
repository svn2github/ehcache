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

package net.sf.ehcache.pool.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.util.ClassLoaderUtil;

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
    private static final String USER_FILTER_RESOURCE = "net.sf.ehcache.sizeof.filter";

    private static final SizeOfFilter DEFAULT_FILTER;
    static {
        Collection<SizeOfFilter> filters = new ArrayList<SizeOfFilter>();
        filters.add(new AnnotationSizeOfFilter());
        try {
            filters.add(new ResourceSizeOfFilter(SizeOfEngine.class.getResource("builtin-sizeof.filter")));
        } catch (IOException e) {
            LOG.warn("Built-in sizeof filter could not be loaded: {}", e);
        }
        SizeOfFilter userFilter = getUserFilter();
        if (userFilter != null) {
            filters.add(getUserFilter());
        }
        DEFAULT_FILTER = new CombinationSizeOfFilter(filters.toArray(new SizeOfFilter[filters.size()]));
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

    private static SizeOfFilter getUserFilter() {
        String userFilterProperty = System.getProperty(USER_FILTER_RESOURCE);

        if (userFilterProperty != null) {
            List<URL> filterUrls = new ArrayList<URL>();
            try {
                filterUrls.add(new URL(userFilterProperty));
            } catch (MalformedURLException e) {
                //ignore
            }
            try {
                filterUrls.add(new File(userFilterProperty).toURL());
            } catch (MalformedURLException e) {
                //ignore
            }
            filterUrls.add(ClassLoaderUtil.getStandardClassLoader().getResource(USER_FILTER_RESOURCE));
            for (URL filterUrl : filterUrls) {
                SizeOfFilter filter;
                try {
                    filter = new ResourceSizeOfFilter(filterUrl);
                    LOG.info("Using user supplied filter @ {}", filterUrl);
                    return filter;
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return null;
    }

    public long sizeOf(final Object key, final Object value, final Object container) {
        long size = sizeOf.deepSizeOf(key, value, container);
        LOG.debug("size of {}/{}/{} -> {}", new Object[]{key, value, container, size});
        return size;
    }
}

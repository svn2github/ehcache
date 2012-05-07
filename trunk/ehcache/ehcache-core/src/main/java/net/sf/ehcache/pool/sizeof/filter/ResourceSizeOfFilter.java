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

package net.sf.ehcache.pool.sizeof.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Filters based on a configuration file
 *
 * @author Chris Dennis
 */
public class ResourceSizeOfFilter implements SizeOfFilter {

    private final Set<String> filteredTerms;

    /**
     * Builds a filter based on the provided configuration URL
     * @param filterData the URL of the configuration
     * @throws IOException if it couldn't read the configuration from the URL
     */
    public ResourceSizeOfFilter(URL filterData) throws IOException {
        if (filterData == null) {
            filteredTerms = Collections.emptySet();
        } else {
            InputStream is = filterData.openStream();
            try {
                Set<String> filtered = new HashSet<String>();
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                try {
                    while (true) {
                        String field = r.readLine();
                        if (field == null) {
                            break;
                        } else {
                            field = field.trim();
                            if (!field.isEmpty() && !field.startsWith("#")) {
                                filtered.add(field);
                            }
                        }
                    }
                    filteredTerms = Collections.unmodifiableSet(filtered);
                } finally {
                    r.close();
                }
            } finally {
                is.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Field> filterFields(Class<?> klazz, Collection<Field> fields) {
        for (Iterator<Field> it = fields.iterator(); it.hasNext();) {
            Field f = it.next();
            if (filteredTerms.contains(f.getDeclaringClass().getName() + "." + f.getName())) {
                it.remove();
            }
        }
        return fields;
    }

    /**
     * {@inheritDoc}
     */
    public boolean filterClass(Class<?> klazz) {
        String klazzName = klazz.getName();
        if (filteredTerms.contains(klazzName)) {
            return false;
        }
        int lastDot = klazzName.lastIndexOf('.');
        if (lastDot >= 0) {
            String packageName = klazzName.substring(0, lastDot);
            if (!packageName.isEmpty() && filteredTerms.contains(packageName)) {
                return false;
            }
        }
        return true;
    }
}

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

package net.sf.ehcache.search.parser;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Alex Snaps
 */
public class CacheValue implements Serializable {

    private static final long serialVersionUID = -7902875028420080125L;
    final Object value;
    final Map<String, Object> nvPairs = new HashMap<String, Object>();

    public CacheValue(final Object value) {
        this.value = value;
    }

    public CacheValue(final Object value, final Map<String, Object> nvPairs) {
        this.value = value;
        if (nvPairs != null) {
            for (Map.Entry<String, Object> entry : nvPairs.entrySet()) {
                this.nvPairs.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public Object getValue() {
        return value;
    }

    public Object getValue(final String key) {
        return nvPairs.get(key);
    }


    public Map<String, Object> getNvPairs() {
        return Collections.unmodifiableMap(nvPairs);
    }
}
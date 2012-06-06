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

package net.sf.ehcache.loader;

import net.sf.ehcache.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Written for Dead-lock poc
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class ComponentCLoader extends BaseComponentLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentCLoader.class.getName());

    /**
     * @return
     */
    public String getName() {
        return "LoaderC";
    }

    /**
     * @param key
     * @return
     * @throws net.sf.ehcache.CacheException
     */
    public Object load(Object key) throws CacheException {
        LOG.info("Loading Component C({})", key);
        return new ComponentC(key);
    }

    @Override
    public Map loadAll(Collection keys, Object argument) throws CacheException {
        Map result = new HashMap();

        for (Object key : keys) {
            result.put(key, load(key));
        }

        return result;
    }
}

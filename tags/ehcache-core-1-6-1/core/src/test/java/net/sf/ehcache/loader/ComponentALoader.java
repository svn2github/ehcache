/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Written for Dead-lock poc
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class ComponentALoader extends BaseComponentLoader {

    private static final Logger LOG = Logger.getLogger(ComponentALoader.class.getName());

    /**
     * @return
     */
    public String getName() {
        return "LoaderA";
    }

    /**
     * @param arg0
     * @return
     * @throws CacheException
     */
    public Object load(Object arg0) throws CacheException {
        String key = null;
        ComponentA a = null;
        ComponentB b = null;

        boolean createDeadLock = Boolean.valueOf(props.getProperty("createDeadLock")).booleanValue();

        LOG.log(Level.INFO, "createDeadLock=" + createDeadLock);

        if (createDeadLock) {
            key = (String) arg0;
        } else {
            key = new String((String) arg0);
        }

        LOG.log(Level.INFO, "Getting componentB...");
        b = (ComponentB) CacheHelper.get("ehcache-loaderinteractions.xml", "BCache", key);

        return new ComponentA(key, b);

    }

}

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

package net.sf.ehcache.distribution.jgroups;

import java.util.Properties;
import java.util.logging.Logger;


import net.sf.ehcache.CacheManager;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CacheManagerPeerProviderFactory;
import net.sf.ehcache.util.PropertyUtil;

/**
 * @author Pierre Monestie (pmonestie__REMOVE__THIS__@gmail.com)
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */

public class JGroupsCacheManagerPeerProviderFactory extends CacheManagerPeerProviderFactory {
    private static final Logger LOG = Logger.getLogger(JGroupsCacheManagerPeerProviderFactory.class.getName());
    private static final String CONNECT = "connect";

    /**
     * {@inheritDoc}
     */
    public CacheManagerPeerProvider createCachePeerProvider(CacheManager cacheManager, Properties properties) {
        LOG.fine("CREATING JGOUPS PEER PROVIDER");
        String connect = PropertyUtil.extractAndLogProperty(CONNECT, properties);
        if (connect == null) {
            connect = "";
        }

        connect = connect.replaceAll(" ", "");
        if (connect.trim().equals("")) {
            connect = null;
        }
        LOG.fine("Connect is:" + connect);
        return new JGroupManager(cacheManager, connect);

    }

}

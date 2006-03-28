/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.distribution;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.AbstractCacheTest;

/**
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: ManualRMIPeerProviderTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class ManualRMIPeerProviderTest extends MulticastRMIPeerProviderTest {


    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed2.xml");
    }


}

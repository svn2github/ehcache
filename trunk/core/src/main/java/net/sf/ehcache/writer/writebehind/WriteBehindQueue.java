/**
 *  Copyright 2003-2009 Terracotta, Inc.
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
package net.sf.ehcache.writer.writebehind;

import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;

/**
 * An implementation of write behind with a queue that is kept in non durable local heap.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class WriteBehindQueue implements WriteBehind {
    /**
     * {@inheritDoc}
     */
    public void start(CacheWriter writer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public void write(Element element) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public void delete(Object key) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

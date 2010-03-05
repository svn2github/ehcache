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

package net.sf.ehcache.store.compound.impl;

import java.util.concurrent.ExecutorService;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.compound.ElementProxy;
import net.sf.ehcache.store.compound.ElementProxyFactory;
import net.sf.ehcache.store.compound.LocalStore;

/**
 * A mock-up of a on-disk element proxy factory.
 * 
 * @author Chris Dennis
 */
public class DiskStorageFactory implements ElementProxyFactory<ElementProxy> {

    /**
     * Executor service used to write elements to disk
     */
    private final ExecutorService diskWriter = null;

    /**
     * Store instance that we construct proxies for.
     */
    private final LocalStore store = null;
    
    /**
     * Encodes an Element as a marker to on-disk location.
     * <p>
     * Immediately substitutes a placeholder for the original
     * element while the Element itself is asynchronously written
     * to disk using the executor service.
     */    
    public ElementProxy encode(Object key, Element element) {
        Placeholder p = new Placeholder(key, element);
        diskWriter.execute(new DiskWriteTask(p));
        return p;
    }
    
    /**
     * Decode an ElementProxy from an on disk marker (or a pending placeholder).
     * <p>
     * This implementation makes no attempt to fault in the decoded 
     * Element in place of the proxy.
     */
    public Element decode(Object key, ElementProxy proxy) {
        if (proxy instanceof DiskMarker) {
            return read((DiskMarker) proxy);
        } else {
            return ((Placeholder) proxy).element;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * For a DiskMarker instance this frees the associated disk space used
     * to store the Element.
     */
    public void free(ElementProxy proxy) {
        if (proxy instanceof DiskMarker) {
            free((DiskMarker) proxy);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void freeAll() {
        // TODO Auto-generated method stub        
    }
    
    private Element read(DiskMarker marker) {
        throw new UnsupportedOperationException();
    }

    private DiskMarker write(Element element) {
        throw new UnsupportedOperationException();
    }

    private void free(DiskMarker marker) {
        throw new UnsupportedOperationException();
    }

    /**
     * Placeholder instances are put in place to prevent
     * duplicate write requests while Elements are being
     * written to disk.
     */
    private class Placeholder implements ElementProxy {
        protected final Object key;
        protected final Element element;
        
        Placeholder(Object key, Element element) {
            this.key = key;
            this.element = element;
        }

        public DiskStorageFactory getFactory() {
            return DiskStorageFactory.this;
        }
    }
    
    /**
     * DiskMarker instances point to the location of their
     * associated serialized Element instance.
     */
    private class DiskMarker implements ElementProxy {

        public DiskStorageFactory getFactory() {
            return DiskStorageFactory.this;
        }
    }

    /**
     * DiskWriteTasks are used to serialize elements
     * to disk and fault in the resultant DiskMarker
     * instance.
     */
    private class DiskWriteTask implements Runnable {

        protected final Placeholder placeholder;
        
        DiskWriteTask(Placeholder p) {
            this.placeholder = p;
        }

        public void run() {
            DiskMarker marker = write(placeholder.element);
            if (!store.fault(placeholder.key, placeholder, marker)) {
                free(marker);
            }
        }
    }
}

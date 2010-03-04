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

public class DiskStorageFactory extends ElementProxyFactory {

    private final ExecutorService diskWriter = null;
    
    private final LocalStore store = null;
    
    @Override
    public ElementProxy encode(Object key, Element element) {
        Placeholder p = new Placeholder(key, element);
        diskWriter.execute(new DiskWriteTask(p));
        return p;
    }
    
    @Override
    public Element decode(Object key, ElementProxy proxy) {
        if (proxy instanceof DiskMarker) {
            return read((DiskMarker) proxy);
        } else {
            return ((Placeholder) proxy).element;
        }
    }

    @Override
    public void free(ElementProxy proxy) {
        if (proxy instanceof DiskMarker) {
            free((DiskMarker) proxy);
        }
    }

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
    
    class Placeholder implements ElementProxy {
        final Object key;
        final Element element;
        
        public Placeholder(Object key, Element element) {
            this.key = key;
            this.element = element;
        }

        public ElementProxyFactory getFactory() {
            return DiskStorageFactory.this;
        }
    }
    
    class DiskMarker {

    }

    class DiskWriteTask implements Runnable {

        final Placeholder placeholder;
        
        public DiskWriteTask(Placeholder p) {
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

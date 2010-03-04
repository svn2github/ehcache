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

package net.sf.ehcache.store.compound;

import net.sf.ehcache.Element;

public abstract class ElementProxyFactory<T extends ElementProxy> implements InternalElementProxyFactory {

    public abstract T encode(Object key, Element element);

    public abstract Element decode(Object key, T proxy);
    
    public abstract void free(T proxy);

    public final Element decode(Object key, Object object) {
        return decode(key, (T) object);
    }
    
    public final void free(Object object) {
        free(object);
    }
}

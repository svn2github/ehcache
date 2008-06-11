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

package net.sf.ehcache.server.jaxb;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Greg Luck
 * @version $Id$
 */
@Provider
public final class JAXBContextResolver implements ContextResolver<JAXBContext> {

    private final JAXBContext context;

    private final Set<Class> types;

    private final Class[] classTypes = {Caches.class, Cache.class, Element.class};

    /**
     *
     * @throws Exception
     */
    public JAXBContextResolver() throws Exception {
        this.types = new HashSet(Arrays.asList(classTypes));
        this.context = JAXBContext.newInstance(classTypes);
    }

    /**
     *
     * @param objectType
     * @return
     */
    public JAXBContext getContext(Class<?> objectType) {
        return (types.contains(objectType)) ? context : null;
    }
}


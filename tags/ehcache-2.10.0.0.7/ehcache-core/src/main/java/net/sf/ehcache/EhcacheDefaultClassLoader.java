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

package net.sf.ehcache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

public class EhcacheDefaultClassLoader extends ClassLoader {
    
    private static final ClassLoader INSTANCE = new EhcacheDefaultClassLoader();
    
    public static ClassLoader getInstance() {
        return INSTANCE;
    }
    
    private final ClassLoader ehcacheLoader = EhcacheDefaultClassLoader.class.getClassLoader();

    private EhcacheDefaultClassLoader() {
        super(null);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            try {
                return tccl.loadClass(name);
            } catch (ClassNotFoundException cnfe) {
                //
            }
        }

        return ehcacheLoader.loadClass(name);
    }

    @Override
    public URL getResource(String name) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            URL url = tccl.getResource(name);
            if (url != null) {
                return url;
            }
        }

        return ehcacheLoader.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL resource = getResource(name);
        try {
            return resource == null ? null : resource.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            Enumeration<URL> urls = tccl.getResources(name);
            if (urls != null && urls.hasMoreElements()) {
                return urls;
            }
        }

        return ehcacheLoader.getResources(name);
    }
}

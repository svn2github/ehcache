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
package net.sf.ehcache.management;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceClassLoader can load classes nested in a subdirectory of a jar
 * Example :
 * ehcache.jar!/net/sf/ehcache/CacheManager is in the "normal" classpath and will be loaded by any typical classloader
 * ehcache.jar!/subdirectory/net/sf/ehcache/CacheManager can only be loaded by the ResourceClassLoader, with prefix "subdirectory"
 *
 * @author Anthony Dahanne
 *
 */
public class ResourceClassLoader extends ClassLoader {

    private static final int BUFFER_SIZE = 1024;
    private static final Logger LOG = LoggerFactory.getLogger(ResourceClassLoader.class);
    private final String prefix;
    private final String implementationVersion;

    /**
     * Given a parent classloader and the prefix to apply to the lookup path
     * Creates a ResourceClassLoader able to load classes from resources prefixed with "prefix"
     *
     * @param prefix
     * @param parent
     * @throws IOException
     */
    public ResourceClassLoader(String prefix, ClassLoader parent) {
        super(parent);
        this.prefix = prefix;
        String temporaryImplementationVersion = null;
        InputStream in = null;
        // looking up the version of our jar, from the Manifest in the private package (prefix)
        try {
            URL manifestResource = getParent().getResource(prefix + "/META-INF/MANIFEST.MF");
            in = manifestResource.openStream();
            Manifest man = new Manifest(in);
            Attributes attributes = man.getMainAttributes();
            temporaryImplementationVersion = attributes.getValue(Name.IMPLEMENTATION_VERSION);
        } catch (Exception e) {
            LOG.warn("Could not read the Manifest", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
              } catch (Exception e) {
                  /* Ignore */
              }
        }
        this.implementationVersion = temporaryImplementationVersion;
    }

    @Override
    public synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // changing the order of delegation to prefer the resourceClassLoader over its parents
        Class c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                c = super.loadClass(name, resolve);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    public URL getResource(String name) {
        URL url;
        url = findResource(name);
        if (url == null) {
            return super.getResource(name);
        }
        return url;
    }

    @Override
    protected URL findResource(String name) {
        URL resource = getParent().getResource(prefix + "/" + name);
        return resource;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> resources = getParent().getResources(prefix + "/" + name);
        return resources;
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {


        String classRealName = prefix + "/" + className.replace('.', '/') + ".class";
        URL classResource = getParent().getResource(classRealName);

        if (classResource != null) {
            // classresource ok, let's define its package too
            int index = className.lastIndexOf('.');
            if (index != -1) {
                String pkgname = className.substring(0, index);
                if (getPackage(pkgname) == null) {
                    definePackage(pkgname, null, null, null, null, implementationVersion, null, null);
                }
            }
            InputStream in = null;
            try {
                byte[] array = new byte[BUFFER_SIZE];
                in = classResource.openStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream(array.length);
                int length = in.read(array);
                while (length > 0) {
                    out.write(array, 0, length);
                    length = in.read(array);
                }
                Class<?> defineClass = defineClass(className, out.toByteArray(), 0, out.size());
                return defineClass;
            } catch (IOException e) {
                LOG.warn("Impossible to open " + classRealName + " for loading", e);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                  } catch (Exception e) {
                      /* Ignore */
                  }
            }
        }
        throw new ClassNotFoundException(className);

    }

}

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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import net.sf.ehcache.util.MergedEnumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceClassLoader can load classes nested in a subdirectory of a jar
 * Example :
 * ehcache.jar!/net/sf/ehcache/CacheManager is in the "normal" classpath and will be loaded by any typical classloader
 * ehcache.jar!/subdirectory/net/sf/ehcache/CacheManager can only be loaded by the ResourceClassLoader, with prefix "subdirectory"
 *
 * Assumes classes under prefix directory to have ending .clazz
 *
 * @author Anthony Dahanne
 *
 */
public class ResourceClassLoader extends ClassLoader {
    private static final String PRIVATE_CLASS_SUFFIX = ".class_terracotta";
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
            LOG.debug("Could not read the Manifest", e);
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
        if (name.startsWith("java.")) {
            return getParent().loadClass(name);
        }

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
        String resource = replaceWithPrivateSuffix(name);
        return getParent().getResource(prefix + "/" + resource);
    }

    @Override
    /**
     * very similar to what OracleJDK classloader does,
     * except the first resources (more important) are the ones found with our ResourceClassLoader
     */
    public Enumeration<URL> getResources(String resourceName) throws IOException {
        Enumeration[] tmp = new Enumeration[2];
        tmp[0] = findResources(resourceName);
        tmp[1] = getParent().getResources(resourceName);
        return new MergedEnumeration<URL>(tmp[0], tmp[1]);
    };

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        String resource = replaceWithPrivateSuffix(name);
        Enumeration<URL> resources = getParent().getResources(prefix + "/" + resource);
        // DEV-8100 add support for Jboss AS, translating vfs URLs
        List<URL> urls = new ArrayList<URL>();
        while (resources.hasMoreElements()) {
            URL elementToAdd;
            URL nextElement = resources.nextElement();
            if (nextElement.toExternalForm().startsWith("vfs")) {
                elementToAdd = translateFromVFSToPhysicalURL(nextElement);
            } else {
                elementToAdd = nextElement;
            }
            urls.add(elementToAdd);
        }
        return Collections.enumeration(urls);
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {

        String classRealName = prefix + "/" + className.replace('.', '/') + PRIVATE_CLASS_SUFFIX;
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

    /**
     * DEV-8100 add support for Jboss AS
     * jersey does not understand Jboss VFS URLs , so we use Jboss VFS classes to translate those URLs to file: URLs
     */
    private URL translateFromVFSToPhysicalURL(URL vfsUrl) throws IOException {
        URL physicalUrl = null;
        URLConnection vfsURLConnection = vfsUrl.openConnection();
        Object vfsVirtualFile = vfsURLConnection.getContent();
        try {
            Class vfsUtilsClass = Class.forName("org.jboss.vfs.VFSUtils");
            Class virtualFileClass = Class.forName("org.jboss.vfs.VirtualFile");
            Method getPathName = virtualFileClass.getDeclaredMethod("getPathName", new Class[0]);
            Method getPhysicalURL = vfsUtilsClass.getDeclaredMethod("getPhysicalURL", virtualFileClass);
            Method recursiveCopy = vfsUtilsClass.getDeclaredMethod("recursiveCopy", virtualFileClass, File.class);
            String pathName = (String) getPathName.invoke(vfsVirtualFile, (Object[]) null);
            physicalUrl = (URL) getPhysicalURL.invoke(null, vfsVirtualFile);
            File physicalURLAsFile = new File(physicalUrl.getFile());
            // https://issues.jboss.org/browse/JBAS-8786
            if (physicalURLAsFile.isDirectory() && physicalURLAsFile.list().length == 0) {
                // jboss does not unpack the libs in WEB-INF/lib, we have to unpack them (partially) ourselves
                unpackVfsResourceToPhysicalURLLocation(physicalUrl, vfsVirtualFile, recursiveCopy);
            }
        } catch (ClassNotFoundException e) {
            // jboss-5 and below doesn't have this class, so just return the vfsUrl,
            // in case the library loading this resource knows how to handle it
          physicalUrl = vfsUrl;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        return physicalUrl;
    }

    private void unpackVfsResourceToPhysicalURLLocation(URL physicalUrl, Object vfsVirtualFile, Method recursiveCopy)
            throws IllegalAccessException, InvocationTargetException {
        String physicalPath = physicalUrl.getFile() + "/../";
        recursiveCopy.invoke(null, vfsVirtualFile, new File(physicalPath));
    }

    private String replaceWithPrivateSuffix(String name) {
        return name.endsWith(".class") ? name.substring(0, name.lastIndexOf(".class")) + PRIVATE_CLASS_SUFFIX : name;
    }

}

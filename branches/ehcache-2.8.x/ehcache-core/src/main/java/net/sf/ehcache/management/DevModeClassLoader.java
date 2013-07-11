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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.ehcache.util.MergedEnumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classloader used for devmode to load management classes
 * Substitutes for ResourceClassLoader
 * 
 * @author hhuynh
 * 
 */
public class DevModeClassLoader extends ClassLoader {
    /**
     * devmode resource file containing Maven dependencies of rest agent jar
     */
    private static final String DEVMODE_OS_DEPENDENCIES_RESOURCE = "/META-INF/devmode/net.sf.ehcache.internal/ehcache-rest-agent/dependencies.txt";
    private static final String DEVMODE_EE_DEPENDENCIES_RESOURCE = "/META-INF/devmode/net.sf.ehcache.internal/ehcache-ee-rest-agent/dependencies.txt";

    private static final Pattern ARTIFACT_PATTERN = Pattern.compile("^\\s*([^:]+):([^:]+):([^:]+):([^:]+):(.+)$");
    private static final Logger LOG = LoggerFactory.getLogger(DevModeClassLoader.class);

    private final URLClassLoader urlClassLoader;

    /**
     * constructor with a classloader parent
     * 
     * @param parent
     */
    public DevModeClassLoader(URL depsReource, ClassLoader parent) {
        super(parent);
        urlClassLoader = initUrlClassLoader(depsReource);
    }
    
    /**
     * returns either EE or OS resource file that contains rest agent dependencies
     * null if not found
     * @return
     */
    public static URL devModeResource() {
        URL url = DevModeClassLoader.class.getResource(DEVMODE_EE_DEPENDENCIES_RESOURCE);
        if (url != null) {
            return url;
        }
        url = DevModeClassLoader.class.getResource(DEVMODE_OS_DEPENDENCIES_RESOURCE);
        if (url != null) {
            return url;
        }
        return null;
    }
    
    private URLClassLoader initUrlClassLoader(URL depsReource) {
        List<URL> urlList = new ArrayList<URL>();
        InputStream in = null;
        try {
            in = depsReource.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = reader.readLine()) != null) {
                Matcher m = ARTIFACT_PATTERN.matcher(line);
                if (m.matches()) {
                    URL url = constructMavenLocalFile(m.group(1), m.group(2), m.group(3), m.group(4));
                    LOG.debug("devmode jar: " + url);
                    urlList.add(url);
                }
            }

            return new URLClassLoader((URL[]) urlList.toArray(new URL[0]), getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }

    private URL constructMavenLocalFile(String groupId, String artifactId, String type, String version) {
        String base = System.getProperty("localMavenRepository");
        if (base == null) {
            base = System.getProperty("user.home") + "/.m2/repository";
        }
        File artifact = new File(base, groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version
                + "." + type);
        if (!artifact.exists()) {
            throw new AssertionError("Can't find Maven artifact: " + groupId + ":" + artifactId + ":" + type + ":" + version);
        }
        try {
            return artifact.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
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
                c = getParent().loadClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return urlClassLoader.loadClass(name);
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (url == null) {
            return super.getResource(name);
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration[] tmp = new Enumeration[2];
        tmp[0] = findResources(name);
        tmp[1] = getParent().getResources(name);
        return new MergedEnumeration<URL>(tmp[0], tmp[1]);
    }

    @Override
    protected URL findResource(String name) {
        return urlClassLoader.findResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return urlClassLoader.findResources(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL resource = getResource(name);
        if (resource != null) {
            try {
                return resource.openStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}

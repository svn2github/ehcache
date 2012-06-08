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

public class ResourceClassLoader extends ClassLoader {

    private final String prefix;
    private final String implementationVersion;
    private static final Logger LOG = LoggerFactory.getLogger(ResourceClassLoader.class);

    public ResourceClassLoader(String prefix, ClassLoader parent) throws IOException {
        super(parent);
        this.prefix = prefix;
        String implementationVersion = null;
        // looking up the version of our jar, from the Manifest in the private package (prefix)
        try {
            URL manifestResource = getParent().getResource(prefix + "/META-INF/MANIFEST.MF");
            InputStream in = manifestResource.openStream();
            Manifest man = new Manifest(in);
            Attributes attributes = man.getMainAttributes();
            implementationVersion = attributes.getValue(Name.IMPLEMENTATION_VERSION);
        } catch (Exception e) {
            LOG.warn("Could not read the rest agent Manifest", e);
        }
        this.implementationVersion = implementationVersion;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
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

        int index = className.lastIndexOf('.');
        if (index != -1) {
            String pkgname = className.substring(0, index);
            if (getPackage(pkgname) == null) {
                definePackage(pkgname, null, null, null, null, implementationVersion, null, null);
            }
        }

        String classRealName = prefix + "/" + className.replace('.', '/') + ".class";
        URL classResource = getParent().getResource(classRealName);

        if (classResource != null) {
            try {
                byte[] array = new byte[1024];
                InputStream in = classResource.openStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream(array.length);
                int length = in.read(array);
                while (length > 0) {
                    out.write(array, 0, length);
                    length = in.read(array);
                }
                return defineClass(className, out.toByteArray(), 0, out.size());
            } catch (IOException e) {
                LOG.warn("Impossible to open " + classRealName + " for loading", e);
            }
        }
        throw new ClassNotFoundException();

    }

}

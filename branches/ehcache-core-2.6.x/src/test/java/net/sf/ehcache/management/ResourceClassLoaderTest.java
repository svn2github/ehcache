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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 *
 * Test to validate the behavior of the resource class loader
 * Loading a test jar in /resourceclassloader/private-classpath.jar, with classes stored in a subdirectory :
 * Archive: private-classpath.jar
 * testing: META-INF/ OK
 * testing: META-INF/MANIFEST.MF OK
 * testing: private-classpath/ OK
 * testing: private-classpath/META-INF/ OK
 * testing: private-classpath/META-INF/MANIFEST.MF OK
 * testing: private-classpath/pof/ OK
 * testing: private-classpath/pof/Simple.class OK
 * testing: private-classpath/pof/sub/ OK
 * testing: private-classpath/pof/sub/Hello.class OK
 * testing: private-classpath/toto.txt OK
 * testing: src/ OK
 * testing: src/pof/ OK
 * testing: src/pof/Simple.java OK
 * testing: src/pof/sub/ OK
 * testing: src/pof/sub/Hello.java OK
 *
 * the test uses the ResourceClassLoader to be able to load classes in the "private-classpath" sub directory
 * Simple and Hello classes bytecode was not modified (shaded) before copying them to the sub directory
 *
 * @author Anthony Dahanne
 *
 */
public class ResourceClassLoaderTest {

    private URLClassLoader testCaseClassLoader;

    @Before
    public void setUp() throws MalformedURLException {
        // loading a jar having classes in a special sub directory
        URL privateClassPathJarAsUrl = this.getClass().getResource("/resourceclassloader/private-classpath.jar");
        // creating a classloader having the special jar in its classpath (but not able to find classes in the private classpath)
        testCaseClassLoader = new URLClassLoader(new URL[] {privateClassPathJarAsUrl}, this.getClass().getClassLoader());
    }

    @Test(expected = ClassNotFoundException.class)
    public void contentOfTheJarNotVisibleWithNormalClassLoaderTest() throws ClassNotFoundException {
        // a normal classloader, can not read classes located in the subdirectory of a jar
        Class<?> simpleClass = testCaseClassLoader.loadClass("pof.Simple");
    }

    @Test
    public void workingWithClassFromPrivateClassPathTest() throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {

        // instantiating the resourceclassloader with the testclassloader as its parent
        ResourceClassLoader resourceClassLoader = new ResourceClassLoader("private-classpath", testCaseClassLoader);

        // loading and instantiating the class nested in /resourceclassloader/private-classpath.jar!/private-classpath/pof.Simple.class
        Class<?> simpleClass = resourceClassLoader.loadClass("pof.Simple");
        Constructor<?> simpleClassConstructor = simpleClass.getConstructor(new Class[] {});
        Object simpleObject = simpleClassConstructor.newInstance(new Object[] {});

        // using a method depending on another private classpath class
        Method sayHelloMethod = simpleClass.getMethod("sayHello", new Class[] {});
        Object sayHello = sayHelloMethod.invoke(simpleObject, new Object[] {});
        Assert.assertEquals("hello!", sayHello);

        // retrieving the version of the jar, from the private classpath Manifest
        Method printVersionMethod = simpleClass.getMethod("printVersion", new Class[] {});
        Object printVersion = printVersionMethod.invoke(simpleObject, new Object[] {});
        Assert.assertEquals("2.6.0-SNAPSHOT", printVersion);

        // reading a resource resourceclassloader/private-classpath.jar!/private-classpath/toto.txt in the private classpath
        Method getMessageInTextFileMethod = simpleClass.getMethod("getMessageInTextFile", new Class[] {});
        Object message = getMessageInTextFileMethod.invoke(simpleObject, new Object[] {});
        Assert.assertEquals("Congratulations ! You could read a file from a hidden resource location !", message);
    }

//
//    @Test
//    public void parentClassLoaderIsNull(){
//        ResourceClassLoader resourceClassLoader = new ResourceClassLoader("pof", null);
//
//    }

}

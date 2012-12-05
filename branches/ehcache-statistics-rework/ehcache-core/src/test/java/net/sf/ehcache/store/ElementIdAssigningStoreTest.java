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

package net.sf.ehcache.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Element;
import net.sf.ehcache.ElementIdHelper;
import net.sf.ehcache.store.ElementIdAssigningStore;
import net.sf.ehcache.store.NullStore;
import net.sf.ehcache.store.Store;

import org.junit.Test;

import antlr.collections.List;

public class ElementIdAssigningStoreTest {

    @Test
    public void test() {
        Handler handler = new Handler();
        Store store = (Store) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {Store.class}, handler);

        Element e = new Element("foo", "bar");
        store.put(e);
        assertId(e, 1);

        store.putIfAbsent(e);
        assertId(e, 2);

        Element anotherElement = new Element("tim", "eck");
        store.putAll(Arrays.asList(new Element[] {e, anotherElement}));
        assertId(e, 3);
        assertId(anotherElement, 4);

        store.putWithWriter(e, null);
        assertId(e, 5);

        store.replace(e);
        assertId(e, 6);

        store.replace(null, e, null);
        assertId(e, 7);

        Set<String> methodsTested = handler.getMethodsTested();

        // This isn't perfect, but try to figure out this test case is missing coverage
        // by finding methods that take Element and asserting that they are known
        //
        // If this is failing then perhaps this new method signature should be assigning
        // an ID and needs coverage here, or maybe just added to the known set of ignore signatures
        for (Method storeMethod : Store.class.getMethods()) {
            for (Class paramType : storeMethod.getParameterTypes()) {
                if (Element.class.isAssignableFrom(paramType) || isCollection(paramType) || isElementArray(paramType)) {
                    String sig = makeSig(storeMethod);
                    if (!methodsTested.contains(sig) && !IGNORE.contains(sig)) {
                        throw new AssertionError("New Store method that might need elemenet ID assignment: " + sig);
                    }
                }
            }
        }
    }

    private boolean isElementArray(Class type) {
        if (type.isArray()) {
            Class component = type.getComponentType();
            while (component.getComponentType() != null) {
                component = component.getComponentType();
            }

            return Element.class.isAssignableFrom(component);
        }

        return false;
    }

    private boolean isCollection(Class type) {
        return Map.class.isAssignableFrom(type) || List.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
    }

    private static void assertId(Element e, int expectedId) {
        assertTrue(ElementIdHelper.hasId(e));
        assertEquals(expectedId, ElementIdHelper.getId(e));
    }

    private static String makeSig(Method method) {
        return method.getName() + Arrays.asList(method.getParameterTypes());
    }

    private static class Handler implements InvocationHandler {
        private final Store store = new ElementIdAssigningStore(NullStore.create());
        private final Set<String> methodsCalled = new HashSet<String>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            methodsCalled.add(makeSig(method));
            return method.invoke(store, args);
        }

        public Set<String> getMethodsTested() {
            return methodsCalled;
        }
    }

    private static final Set<String> IGNORE = new HashSet<String>();
    static {
        IGNORE.add("removeAll[interface java.util.Collection]");
        IGNORE.add("removeElement[class net.sf.ehcache.Element, interface net.sf.ehcache.store.ElementValueComparator]");
        IGNORE.add("setAttributeExtractors[interface java.util.Map]");
        IGNORE.add("getAllQuiet[interface java.util.Collection]");
        IGNORE.add("getAll[interface java.util.Collection]");
    }

}

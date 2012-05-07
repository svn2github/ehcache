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

package net.sf.ehcache.search.attribute;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.InvalidConfigurationException;

public class ReflectionAttributeExtractorTest extends TestCase {

    /**
     * Makes sure that any caching done in the extractor is thread safe in the face of heterogenous input types
     */
    public void testHeterogenousTypesThreaded() throws InterruptedException {
        final List<Throwable> errors = new Vector<Throwable>();

        final int NUM = 500000;

        final ReflectionAttributeExtractor method = new ReflectionAttributeExtractor("value.getValue()");
        final ReflectionAttributeExtractor field = new ReflectionAttributeExtractor("value.value");

        class Task implements Runnable {

            private final int type;
            private int count;

            Task(int type) {
                this.type = type;
            }

            public void run() {
                try {
                    for (int i = 0; i < NUM; i++) {
                        assertEquals(count, method.attributeFor(element(), ""));
                        assertEquals(count, field.attributeFor(element(), ""));
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    errors.add(t);
                }
            }

            private Element element() {
                switch (type) {
                    case 0: {
                        return new Element("k", new Person1(count++));
                    }
                    case 1: {
                        return new Element("k", new Person2(count++));
                    }
                    case 2: {
                        return new Element("k", new Person3(count++));
                    }
                    case 3: {
                        return new Element("k", new Person4(count++));
                    }
                    case 4: {
                        return new Element("k", new Person5(count++));
                    }
                    default: {
                        throw new AssertionError(type);
                    }
                }
            }
        }

        Thread threads[] = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Task(i));
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(0, errors.size());
    }

    public void testBasic() {
        Element element = new Element("k", "v");

        ReflectionAttributeExtractor rae;

        rae = new ReflectionAttributeExtractor("element");
        assertEquals(element, rae.attributeFor(element, ""));

        rae = new ReflectionAttributeExtractor("ELEMENT");
        assertEquals(element, rae.attributeFor(element, ""));

        rae = new ReflectionAttributeExtractor("element.getObjectKey()");
        assertEquals("k", rae.attributeFor(element, ""));

        rae = new ReflectionAttributeExtractor("key");
        assertEquals("k", rae.attributeFor(element, ""));

        rae = new ReflectionAttributeExtractor("KEY");
        assertEquals("k", rae.attributeFor(element, ""));

        rae = new ReflectionAttributeExtractor("value");
        assertEquals("v", rae.attributeFor(element, ""));

        rae = new ReflectionAttributeExtractor("VALUE");
        assertEquals("v", rae.attributeFor(element, ""));

        rae = new ReflectionAttributeExtractor("key.toString()");
        assertEquals("k", rae.attributeFor(element, ""));

        rae = new ReflectionAttributeExtractor("value.toString()");
        assertEquals("v", rae.attributeFor(element, ""));

        element = new Element("k", new Ref(new Ref("v")));
        rae = new ReflectionAttributeExtractor("value.reference.reference.toString()");
        assertEquals("v", rae.attributeFor(element, ""));
    }

    public void testInheritedMethodCall() {
        Object value = new NoExplicitToString();
        Element element = new Element("k", value);

        ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("value.toString()");
        assertEquals(value.toString(), rae.attributeFor(element, ""));
    }

    public void testNullInChain() {

        try {
            Element e = new Element("k", new Ref(null));
            ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("value.reference.toString()");
            rae.attributeFor(e, "");
            fail();
        } catch (AttributeExtractorException aee) {
            // expected
        }

        try {
            Element e = new Element("k", new Ref(null));
            ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("value.reference.reference");
            rae.attributeFor(e, "");
            fail();
        } catch (AttributeExtractorException aee) {
            // expected
        }
    }

    public void testMethodThrowsException() {
        RuntimeException re = new RuntimeException();
        try {
            Ref ref = new Ref(re);
            ref.re = re;
            Element e = new Element("k", ref);

            ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("value.exception()");
            rae.attributeFor(e, "");
            fail();
        } catch (AttributeExtractorException aee) {
            assertEquals(re, aee.getCause());
        }
    }

    public void testInheritedField() {
        Element element = new Element("k", new Sub());

        ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("value.field");
        assertEquals("base", rae.attributeFor(element, ""));
    }

    public void testExceptions() {
        Element element = new Element("k", "v");

        try {
            ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("value.FIELD_DOES_NOT_EXIST");
            rae.attributeFor(element, "");
            fail();
        } catch (AttributeExtractorException aee) {
            // expected
        }

        try {
            ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("value.METHOD_DOES_NOT_EXIST()");
            rae.attributeFor(element, "");
            fail();
        } catch (AttributeExtractorException aee) {
            // expected
        }
    }

    public void testWhitespaceIgnored() {
        Element element = new Element("k", "v");

        ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("   value.toString()   ");
        assertEquals("v", rae.attributeFor(element, ""));
    }

    public void testInvalidExpressions() {
        tryInvalidExpression("");
        tryInvalidExpression("  ");
        tryInvalidExpression("tim");
        tryInvalidExpression("element1.toString()");
        tryInvalidExpression("element..toString()");
        tryInvalidExpression("element.1ref");
        tryInvalidExpression("element . toString()");
    }

    private void tryInvalidExpression(String expr) {
        try {
            new ReflectionAttributeExtractor(expr);
            fail(expr);
        } catch (InvalidConfigurationException ice) {
            // expected
        }
    }

    private static class NoExplicitToString {
        // this class does not directly declare toString()

        static {
            for (Method m : NoExplicitToString.class.getDeclaredMethods()) {
                if (m.getName().equals("toString()")) {
                    throw new AssertionError(m);
                }
            }
        }

    }

    private static class Base {
        private final Object field = "base";
    }

    private static class Sub extends Base {
        //
    }

    private static class Ref {
        public RuntimeException re;

        public Ref(Object ref) {
            this.reference = ref;
        }

        public void exception() {
            if (re != null) {
                throw re;
            }
        }

        public Object reference;
    }

    private static class Person1 {
        private final int value;

        Person1(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static class Person2 {
        private final int value;

        Person2(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static class Person3 {
        private final int value;

        Person3(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static class Person4 {
        private final int value;

        Person4(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static class Person5 {
        private final int value;

        Person5(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

}

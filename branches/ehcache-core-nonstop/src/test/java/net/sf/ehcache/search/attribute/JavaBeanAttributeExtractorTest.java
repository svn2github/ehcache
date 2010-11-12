/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

import java.util.Vector;

import junit.framework.TestCase;
import net.sf.ehcache.Element;

public class JavaBeanAttributeExtractorTest extends TestCase {

    public void testBasic() {
        JavaBeanAttributeExtractor jbae = new JavaBeanAttributeExtractor("foo");

        assertEquals("foo", jbae.attributeFor(new Element("", new Type1())));
        assertEquals("foo", jbae.attributeFor(new Element(null, new Type1())));
        assertEquals("foo", jbae.attributeFor(new Element(new Type1(), "")));
        assertEquals("foo", jbae.attributeFor(new Element(new Type1(), null)));

        assertEquals(true, jbae.attributeFor(new Element(new Type2(), "")));
        assertEquals(true, jbae.attributeFor(new Element("", new Type2())));

        assertEquals(true, jbae.attributeFor(new Element(new Type3(), "")));
        assertEquals(true, jbae.attributeFor(new Element("", new Type3())));
    }

    public void testException() {
        JavaBeanAttributeExtractor jbae = new JavaBeanAttributeExtractor("foo");

        RuntimeException re = new RuntimeException();
        try {
            jbae.attributeFor(new Element(new ExceptionThrowing(re), ""));
            fail();
        } catch (AttributeExtractorException aee) {
            assertEquals(re, aee.getCause());
        }

        Error error = new Error();
        try {
            jbae.attributeFor(new Element(new ExceptionThrowing(error), ""));
            fail();
        } catch (Error e) {
            assertEquals(error, e);
        }
    }

    public void testNonAccessibleMethod() {
        JavaBeanAttributeExtractor jbae = new JavaBeanAttributeExtractor("foo");

        try {
            jbae.attributeFor(new Element(new NonAccessible(), ""));
            fail();
        } catch (AttributeExtractorException aee) {
            // expected
        }

        try {
            jbae.attributeFor(new Element("", new NonAccessible()));
            fail();
        } catch (AttributeExtractorException aee) {
            // expected
        }
    }

    public void testMultiThreads() throws InterruptedException {
        final JavaBeanAttributeExtractor jbae = new JavaBeanAttributeExtractor("foo");
        final Vector<Throwable> errors = new Vector<Throwable>();

        Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < 50000; j++) {
                            assertEquals("foo", jbae.attributeFor(new Element("", new Type1())));
                            assertEquals("foo", jbae.attributeFor(new Element(null, new Type1())));
                            assertEquals("foo", jbae.attributeFor(new Element(new Type1(), "")));
                            assertEquals("foo", jbae.attributeFor(new Element(new Type1(), null)));

                            assertEquals(true, jbae.attributeFor(new Element(new Type2(), "")));
                            assertEquals(true, jbae.attributeFor(new Element("", new Type2())));

                            assertEquals(true, jbae.attributeFor(new Element(new Type3(), "")));
                            assertEquals(true, jbae.attributeFor(new Element("", new Type3())));
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        errors.add(t);
                    }
                };
            };
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(0, errors.size());
    }

    public void testParentType() {
        JavaBeanAttributeExtractor jbae1 = new JavaBeanAttributeExtractor("bar");
        JavaBeanAttributeExtractor jbae2 = new JavaBeanAttributeExtractor("wrapper");
        JavaBeanAttributeExtractor jbae3 = new JavaBeanAttributeExtractor("primitive");

        assertEquals("bar", jbae1.attributeFor(new Element(new Type4(), "")));
        assertEquals("bar", jbae1.attributeFor(new Element("", new Type4())));

        assertEquals(true, jbae2.attributeFor(new Element("", new Type4())));
        assertEquals(true, jbae2.attributeFor(new Element(new Type4(), "")));

        assertEquals(true, jbae3.attributeFor(new Element("", new Type4())));
        assertEquals(true, jbae3.attributeFor(new Element(new Type4(), "")));
    }

    public void testAmbiguous() {
        JavaBeanAttributeExtractor jbae = new JavaBeanAttributeExtractor("foo");

        try {
            jbae.attributeFor(new Element(new Type1(), new Type1()));
            fail();
        } catch (AttributeExtractorException aee) {
            // expected
        }
    }

    public void testNoMethods() {
        JavaBeanAttributeExtractor jbae = new JavaBeanAttributeExtractor("foo");

        try {
            jbae.attributeFor(new Element(new Object(), ""));
            fail();
        } catch (AttributeExtractorException aee) {
            // expected
        }

        try {
            jbae.attributeFor(new Element("", new Object()));
            fail();
        } catch (AttributeExtractorException aee) {
            // expected
        }

        try {
            jbae.attributeFor(new Element(null, null));
            fail();
        } catch (AttributeExtractorException aee) {
            // expected
        }
    }

    public void testIllegalArgs() {
        try {
            new JavaBeanAttributeExtractor(null);
            fail();
        } catch (NullPointerException npe) {
            // expected
        }

        try {
            new JavaBeanAttributeExtractor("");
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    private static class Type1 {
        public Object getFoo() {
            return "foo";
        }
    }

    private static class Type2 {
        public boolean isFoo() {
            return true;
        }
    }

    private static class Type3 {
        public Boolean isFoo() {
            return true;
        }
    }

    private static abstract class Parent {
        public boolean isPrimitive() {
            return true;
        }

        public Boolean isWrapper() {
            return true;
        }

        public Object getBar() {
            return "bar";
        }
    }

    private static class Type4 extends Parent {
        //
    }

    private static class NonAccessible {
        Object getFoo() {
            return "foo";
        }
    }

    private static class ExceptionThrowing {
        private final Throwable t;

        public ExceptionThrowing(Throwable t) {
            this.t = t;
        }

        public Object getFoo() {
            HackExceptionThrower.t.set(t);

            try {
                HackExceptionThrower.class.newInstance();
                throw new AssertionError();
            } catch (InstantiationException e) {
                throw new AssertionError();
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }
    }

    private static class HackExceptionThrower {
        public static final ThreadLocal<Throwable> t = new ThreadLocal<Throwable>();

        public HackExceptionThrower() throws Throwable {
            throw t.get();
        }
    }

}

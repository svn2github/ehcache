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

import java.lang.reflect.Method;

import junit.framework.TestCase;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.InvalidConfigurationException;

public class ReflectionAttributeExtractorTest extends TestCase {

    public void testBasic() {
        Element element = new Element("k", "v");

        ReflectionAttributeExtractor rae;

        rae = new ReflectionAttributeExtractor("element");
        assertEquals(element, rae.attributeFor(element));

        rae = new ReflectionAttributeExtractor("element.getObjectKey()");
        assertEquals("k", rae.attributeFor(element));

        rae = new ReflectionAttributeExtractor("key");
        assertEquals("k", rae.attributeFor(element));

        rae = new ReflectionAttributeExtractor("value");
        assertEquals("v", rae.attributeFor(element));

        rae = new ReflectionAttributeExtractor("key.toString()");
        assertEquals("k", rae.attributeFor(element));

        rae = new ReflectionAttributeExtractor("value.toString()");
        assertEquals("v", rae.attributeFor(element));

        element = new Element("k", new Ref(new Ref("v")));
        rae = new ReflectionAttributeExtractor("value.reference.reference.toString()");
        assertEquals("v", rae.attributeFor(element));
    }

    public void testInheritedMethodCall() {
        Object value = new NoExplicitToString();
        Element element = new Element("k", value);

        ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("value.toString()");
        assertEquals(value.toString(), rae.attributeFor(element));
    }

    public void testInheritedField() {
        Element element = new Element("k", new Sub());

        ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("value.field");
        assertEquals("base", rae.attributeFor(element));
    }

    public void testWhitespaceIgnored() {
        Element element = new Element("k", "v");

        ReflectionAttributeExtractor rae = new ReflectionAttributeExtractor("   value.toString()   ");
        assertEquals("v", rae.attributeFor(element));
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
        private Object field = "base";
    }

    private static class Sub extends Base {
        //
    }


    private static class Ref {
        public Ref(Object ref) {
            this.reference = ref;
        }

        public Object reference;
    }

}

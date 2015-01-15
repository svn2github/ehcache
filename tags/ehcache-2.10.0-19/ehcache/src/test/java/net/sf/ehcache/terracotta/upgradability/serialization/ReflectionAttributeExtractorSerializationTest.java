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

package net.sf.ehcache.terracotta.upgradability.serialization;

import java.io.IOException;
import java.util.Comparator;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.ReflectionAttributeExtractor;
import org.junit.Test;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class ReflectionAttributeExtractorSerializationTest {
  
  private static final Comparator<ReflectionAttributeExtractor> COMPARATOR = new Comparator<ReflectionAttributeExtractor>() {
    @Override
    public int compare(ReflectionAttributeExtractor o1, ReflectionAttributeExtractor o2) {
      A a = new A(new B(42));
      Element e = new Element(a, a, 9);
      return o1.attributeFor(e, "foo").equals(o2.attributeFor(e, "foo")) ? 0 : -1;
    }
  };
  
  @Test
  public void testMethodsKey() throws IOException, ClassNotFoundException {
    validateSerializedForm(new ReflectionAttributeExtractor("key.b().c()"), COMPARATOR, "serializedforms/ReflectionAttributeExtractorSerializationTest.testMethodsKey.ser");
  }
  
  @Test
  public void testFieldsKey() throws IOException, ClassNotFoundException {
    validateSerializedForm(new ReflectionAttributeExtractor("key.b.c"), COMPARATOR, "serializedforms/ReflectionAttributeExtractorSerializationTest.testFieldsKey.ser");
  }
  
  @Test
  public void testMethodsValue() throws IOException, ClassNotFoundException {
    validateSerializedForm(new ReflectionAttributeExtractor("value.b().c()"), COMPARATOR, "serializedforms/ReflectionAttributeExtractorSerializationTest.testMethodsValue.ser");
  }
  
  @Test
  public void testFieldsValue() throws IOException, ClassNotFoundException {
    validateSerializedForm(new ReflectionAttributeExtractor("value.b.c"), COMPARATOR, "serializedforms/ReflectionAttributeExtractorSerializationTest.testFieldsValue.ser");
  }
  
  @Test
  public void testMethodsElement() throws IOException, ClassNotFoundException {
    validateSerializedForm(new ReflectionAttributeExtractor("element.getVersion()"), COMPARATOR, "serializedforms/ReflectionAttributeExtractorSerializationTest.testMethodsElement.ser");
  }
  
  @Test
  public void testFieldsElement() throws IOException, ClassNotFoundException {
    validateSerializedForm(new ReflectionAttributeExtractor("element.version"), COMPARATOR, "serializedforms/ReflectionAttributeExtractorSerializationTest.testFieldsElement.ser");
  }
  
  
  static class A {
    
    private final B b;
    
    A(B b) {
      this.b = b;
    }
    
    B b() {
      return b;
    }
  }
  
  static class B {
    
    public final int c;
    
    B(int c) {
      this.c = c;
    }
    
    int c() {
      return c;
    }
  }
}

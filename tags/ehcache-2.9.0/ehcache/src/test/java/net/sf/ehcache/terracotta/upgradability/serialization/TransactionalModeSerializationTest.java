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
import org.junit.Test;

import static net.sf.ehcache.config.CacheConfiguration.TransactionalMode.LOCAL;
import static net.sf.ehcache.config.CacheConfiguration.TransactionalMode.XA;
import static net.sf.ehcache.config.CacheConfiguration.TransactionalMode.XA_STRICT;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class TransactionalModeSerializationTest {
  
  @Test
  public void testLocal() throws IOException, ClassNotFoundException {
    validateSerializedForm(LOCAL, "serializedforms/TransactionalModeSerializationTest.testLocal.ser");
  }
  
  @Test
  public void testXa() throws IOException, ClassNotFoundException {
    validateSerializedForm(XA, "serializedforms/TransactionalModeSerializationTest.testXa.ser");
  }
  
  @Test
  public void testXaStrict() throws IOException, ClassNotFoundException {
    validateSerializedForm(XA_STRICT, "serializedforms/TransactionalModeSerializationTest.testXaStrict.ser");
  }
}

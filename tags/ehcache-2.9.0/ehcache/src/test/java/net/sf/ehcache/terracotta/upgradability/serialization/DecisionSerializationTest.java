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

import static net.sf.ehcache.transaction.Decision.COMMIT;
import static net.sf.ehcache.transaction.Decision.IN_DOUBT;
import static net.sf.ehcache.transaction.Decision.ROLLBACK;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class DecisionSerializationTest {
  
  @Test
  public void testInDoubt() throws IOException, ClassNotFoundException {
    validateSerializedForm(IN_DOUBT, "serializedforms/DecisionSerializationTest.testInDoubt.ser");
  }
  
  @Test
  public void testCommit() throws IOException, ClassNotFoundException {
    validateSerializedForm(COMMIT, "serializedforms/DecisionSerializationTest.testCommit.ser");
  }
  
  @Test
  public void testRollback() throws IOException, ClassNotFoundException {
    validateSerializedForm(ROLLBACK, "serializedforms/DecisionSerializationTest.testRollback.ser");
  }
}

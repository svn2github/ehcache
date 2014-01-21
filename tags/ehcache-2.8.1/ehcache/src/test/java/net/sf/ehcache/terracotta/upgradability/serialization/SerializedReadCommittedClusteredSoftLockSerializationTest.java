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
import java.io.Serializable;
import java.util.Comparator;

import net.sf.ehcache.transaction.TransactionID;
import org.junit.Test;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.terracotta.modules.ehcache.transaction.SerializedReadCommittedClusteredSoftLock;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;


/**
 *
 * @author cdennis
 */
public class SerializedReadCommittedClusteredSoftLockSerializationTest {
  
  private static final Comparator<SerializedReadCommittedClusteredSoftLock> COMPARATOR = new Comparator<SerializedReadCommittedClusteredSoftLock>() {
    @Override
    public int compare(SerializedReadCommittedClusteredSoftLock o1, SerializedReadCommittedClusteredSoftLock o2) {
      return new ReflectionEquals(o1, "softLock").matches(o2) ? 0 : -1;
    }
  };
  
  @Test
  public void testBasic() throws IOException, ClassNotFoundException {
    validateSerializedForm(new SerializedReadCommittedClusteredSoftLock(new DummyTransactionID(), "foo"), COMPARATOR, "serializedforms/SerializedReadCommittedClusteredSoftLockSerializationTest.testBasic.ser");
  }
  
  static class DummyTransactionID implements TransactionID {
    @Override
    public boolean equals(Object obj) {
      return obj instanceof DummyTransactionID;
    }
  }
}

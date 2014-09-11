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

import org.junit.Test;
import org.terracotta.modules.ehcache.writebehind.operations.WriteAsyncOperation;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class WriteAsyncOperationSerializationTest {
  
  private static final Comparator<WriteAsyncOperation> COMPARATOR = new Comparator<WriteAsyncOperation>() {
    @Override
    public int compare(WriteAsyncOperation o1, WriteAsyncOperation o2) {
      return o1.getKey().equals(o2.getKey()) && o1.getElement().getObjectKey().equals(o2.getElement().getObjectKey())
              && o1.getElement().getObjectValue().equals(o2.getElement().getObjectValue())
              && o1.getCreationTime() == o2.getCreationTime() ? 0 : -1;
    }
  };
  
  @Test
  public void testBasic() throws IOException, ClassNotFoundException {
    validateSerializedForm(new WriteAsyncOperation(new Element("foo", "bar", 1L, 2L, 3L, 4L, 0L), 42L), COMPARATOR, "serializedforms/WriteAsyncOperationSerializationTest.testBasic.ser");
  }
}

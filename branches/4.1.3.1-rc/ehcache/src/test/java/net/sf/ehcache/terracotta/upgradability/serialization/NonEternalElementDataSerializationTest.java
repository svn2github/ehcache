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
import net.sf.ehcache.NonEternalElementData;
import org.junit.Test;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class NonEternalElementDataSerializationTest {
  
  private static final Comparator<NonEternalElementData> COMPARATOR = new Comparator<NonEternalElementData>() {

    @Override
    public int compare(NonEternalElementData o1, NonEternalElementData o2) {
      return ElementSerializationTest.COMPARATOR.compare(o1.createElement("foo"), o2.createElement("foo"));
    }
  };
  
  @Test
  public void testBasic() throws IOException, ClassNotFoundException {
    validateSerializedForm(new NonEternalElementData(new Element("foo", "bar", 1000L, 2000L, 3000L, 4000L, false, 5000, 6000, 7000L)), COMPARATOR, "serializedforms/NonEternalElementDataSerializationTest.testBasic.ser");
  }
}

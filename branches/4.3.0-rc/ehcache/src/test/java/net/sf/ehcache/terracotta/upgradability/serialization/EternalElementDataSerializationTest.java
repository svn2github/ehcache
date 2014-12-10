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
import net.sf.ehcache.EternalElementData;

import org.junit.Test;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class EternalElementDataSerializationTest {
  
  private static final Comparator<EternalElementData> COMPARATOR = new Comparator<EternalElementData>() {

    @Override
    public int compare(EternalElementData o1, EternalElementData o2) {
      return ElementSerializationTest.COMPARATOR.compare(o1.createElement("foo"), o2.createElement("foo"));
    }
  };
  
  @Test
  public void testBasic() throws IOException, ClassNotFoundException {
    validateSerializedForm(new EternalElementData(new Element("foo", "bar", 1000L, 2000L, 3000L, 4000L, false, 0, 0, 7000L)), COMPARATOR, "serializedforms/EternalElementDataSerializationTest.testBasic.ser");
  }
}

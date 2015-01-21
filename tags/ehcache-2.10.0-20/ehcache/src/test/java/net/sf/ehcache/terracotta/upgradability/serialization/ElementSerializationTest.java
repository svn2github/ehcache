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
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class ElementSerializationTest {

  public static final Comparator<Element> COMPARATOR = new Comparator<Element>() {

    @Override
    public int compare(Element o1, Element o2) {
      return o1.getObjectKey().equals(o2.getObjectKey())
              && o1.getObjectValue().equals(o2.getObjectValue())
              && o1.getVersion() == o2.getVersion()
              && o1.getHitCount() == o2.getHitCount()
              && o1.getTimeToLive() == o2.getTimeToLive()
              && o1.getTimeToIdle() == o2.getTimeToIdle()
              && o1.getCreationTime() == o2.getCreationTime()
              && o1.getLastAccessTime() == o2.getLastAccessTime()
              && o1.getLastUpdateTime() == o2.getLastUpdateTime()
              && o1.usesCacheDefaultLifespan() == o2.usesCacheDefaultLifespan() ? 0 : -1;
    }
  };
  
  @Test
  public void testBasic() throws IOException, ClassNotFoundException {
    validateSerializedForm(new Element("foo", "bar", 1000L, 2000L, 3000L, 4000L, false, 5000, 6000, 7000L), COMPARATOR, "serializedforms/ElementSerializationTest.testBasic.ser");
  }
}

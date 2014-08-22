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
import net.sf.ehcache.store.compound.CopyStrategy;
import net.sf.ehcache.store.compound.LegacyCopyStrategyAdapter;
import org.junit.Test;

import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class LegacyCopyStrategyAdapterSerializationTest {

  private static final Comparator<LegacyCopyStrategyAdapter> COMPARATOR = new Comparator<LegacyCopyStrategyAdapter>() {

    @Override
    public int compare(LegacyCopyStrategyAdapter o1, LegacyCopyStrategyAdapter o2) {
      ClassLoader loader = getClass().getClassLoader();     
      return o1.copyForRead(null, loader).getObjectKey().equals(o2.copyForRead(null, loader).getObjectKey()) ? 0 : -1;
    }
  };
  
  @Test
  public void testBasic() throws IOException, ClassNotFoundException {
    validateSerializedForm(new LegacyCopyStrategyAdapter(new DummyCopyStrategy()), COMPARATOR, "serializedforms/LegacyCopyStrategyAdapterSerializationTest.testBasic.ser");
  }
  
  static class DummyCopyStrategy implements CopyStrategy {
    
    @Override
    public Object copy(Object value) {
      return new Element("foo", null);
    }
  }
}

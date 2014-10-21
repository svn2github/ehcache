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

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.TransactionIDSerializedForm;
import org.junit.Test;
import org.terracotta.modules.ehcache.transaction.ClusteredTransactionID;

import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.terracotta.upgradability.serialization.SerializationUpgradabilityTesting.validateSerializedForm;

/**
 *
 * @author cdennis
 */
public class ClusteredTransactionIDSerializationTest {
  
  private static final Comparator<ClusteredTransactionID> COMPARATOR = new Comparator<ClusteredTransactionID>() {
    @Override
    public int compare(ClusteredTransactionID o1, ClusteredTransactionID o2) {
      return o1.equals(o2) && o1.getOwnerID().equals(o2.getOwnerID()) ? 0 : -1;
    }
  };

  @Test
  public void testBasic() throws Exception {
    Field MANAGERS_MAP = CacheManager.class.getDeclaredField("CACHE_MANAGERS_MAP");
    MANAGERS_MAP.setAccessible(true);
    CacheManager manager = mock(CacheManager.class);
    ((Map) MANAGERS_MAP.get(null)).put("manager", manager);
    TransactionIDFactory txnIdFactory = mock(TransactionIDFactory.class);
    when(txnIdFactory.restoreTransactionID(refEq(new TransactionIDSerializedForm("manager", "cluster", "owner", 42L, 2))))
            .thenReturn(new ClusteredTransactionID("owner", "cluster", "manager", 42L, 2));
    when(manager.getOrCreateTransactionIDFactory()).thenReturn(txnIdFactory);
    validateSerializedForm(new ClusteredTransactionID("owner", "cluster", "manager", 42L, 2), COMPARATOR, "serializedforms/ClusteredTransactionIDSerializationTest.testBasic.ser");
  }
}

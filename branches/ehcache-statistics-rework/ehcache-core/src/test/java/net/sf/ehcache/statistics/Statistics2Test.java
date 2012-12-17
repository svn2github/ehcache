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

package net.sf.ehcache.statistics;

import java.util.Arrays;
import org.junit.Test;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;

import org.terracotta.context.TreeNode;
import org.terracotta.context.query.QueryBuilder;

public class Statistics2Test extends AbstractCacheTest {

    @Test
    public void testSimple() {
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);

        manager.shutdown();

    }

  public static String dumpTree(TreeNode node) {
    return dumpSubtree(0, node);
  }

  public static String dumpSubtree(int indent, TreeNode node) {
    char[] indentChars = new char[indent];
    Arrays.fill(indentChars, ' ');
    StringBuilder sb = new StringBuilder();
    String nodeString = node.toString();
    sb.append(indentChars).append(nodeString).append("\n");
    for (TreeNode child : node.getChildren()) {
      sb.append(dumpSubtree(indent + nodeString.length(), child));
    }
    return sb.toString();
  }
}

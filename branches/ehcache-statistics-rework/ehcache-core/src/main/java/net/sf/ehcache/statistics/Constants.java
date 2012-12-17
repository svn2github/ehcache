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

import java.util.LinkedList;
import java.util.List;

import org.terracotta.context.TreeNode;

public class Constants {

    public static final String NAME_PROP = "name";
    public static final String PROPERTIES_PROP = "properties";

    /**
     * Form string paths from context.
     *
     * @param tn the tn
     * @return the string[]
     */
    public static String[] formStringPathsFromContext(TreeNode tn) {
        LinkedList<String> results = new LinkedList<String>();
        for (List<? extends TreeNode> path : tn.getPaths()) {
            boolean first = true;
            StringBuilder sb = new StringBuilder();
            for (TreeNode n : path) {
                String name = null;
                if (name == null) {
                    name = (String) n.getContext().attributes().get(NAME_PROP);
                }
                if (name == null) {
                    name = n.getContext().identifier().getSimpleName();
                }
                if (!first) {
                    sb.append("/");
                } else {
                    first = false;
                }

                sb.append(name);
            }
            results.add(sb.toString());

        }

        return results.toArray(new String[0]);

    }
}

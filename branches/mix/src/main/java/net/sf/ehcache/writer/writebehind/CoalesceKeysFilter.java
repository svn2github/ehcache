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
package net.sf.ehcache.writer.writebehind;

import net.sf.ehcache.writer.writebehind.operations.KeyBasedOperation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filters the operations by only retaining the latest operations for a given key.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class CoalesceKeysFilter implements OperationsFilter<KeyBasedOperation> {
    /**
     * {@inheritDoc}
     */
    public void filter(List operations, OperationConverter<KeyBasedOperation> converter) {
        final Map mostRecent = new HashMap();
        final List operationsToRemove = new ArrayList();

        // not using an iterator on purpose since the ehcache express types don't support it
        for (int i = 0; i < operations.size(); i++) {
            Object operation = operations.get(i);
            KeyBasedOperation keyBasedOperation = converter.convert(operation);

            if (!mostRecent.containsKey(keyBasedOperation.getKey())) {
                mostRecent.put(keyBasedOperation.getKey(), operation);
            } else {
                Object previousOperation = mostRecent.get(keyBasedOperation.getKey());
                KeyBasedOperation keyBasedPreviousOperation = converter.convert(previousOperation);

                if (keyBasedPreviousOperation.getCreationTime() > keyBasedOperation.getCreationTime()) {
                    operationsToRemove.add(operation);
                } else {
                    operationsToRemove.add(previousOperation);
                    mostRecent.put(keyBasedOperation.getKey(), operation);
                }
            }
        }

        // not using removeAll on purpose since the ehcache express types don't support it
        for (Object operation : operationsToRemove) {
            operations.remove(operation);
        }
    }
}

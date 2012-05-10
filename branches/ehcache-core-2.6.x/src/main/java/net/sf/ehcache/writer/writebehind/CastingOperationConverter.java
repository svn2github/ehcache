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

/**
 * A converter that simply casts an existing {@code KeyBasedOperation} instance.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public final class CastingOperationConverter implements OperationConverter<KeyBasedOperation> {
  
    private static final CastingOperationConverter INSTANCE = new CastingOperationConverter();

    private CastingOperationConverter() {
        // private default constuctor
    }

    /**
     * Singleton retriever static method
     */
    public static CastingOperationConverter getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public KeyBasedOperation convert(Object source) {
        return (KeyBasedOperation) source;
    }
}

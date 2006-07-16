/**
 *  Copyright 2003-2006 Greg Luck
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


package net.sf.ehcache.store;

import java.io.Serializable;


/**
 * Test class to investigate class loading issues
 * @author Greg Luck
 * @version $Id$
 */
public class Primitive implements Serializable {
    public int integerPrimitive;
    public long longPrimitive;
    public byte bytePrimitive;
    public char charPrimitive;
    public boolean booleanPrimitive;

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object object) {
        return object != null
                && object instanceof Primitive
                && ((Primitive) object).integerPrimitive == integerPrimitive
                && ((Primitive) object).longPrimitive == longPrimitive
                && ((Primitive) object).bytePrimitive == bytePrimitive
                && ((Primitive) object).charPrimitive == charPrimitive
                && ((Primitive) object).booleanPrimitive == booleanPrimitive;
    }
}

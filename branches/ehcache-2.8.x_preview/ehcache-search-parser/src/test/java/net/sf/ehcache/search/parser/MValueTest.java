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

package net.sf.ehcache.search.parser;

import net.sf.ehcache.search.parser.MValue.MEnum;

import org.junit.Assert;
import org.junit.Test;

public class MValueTest {

    public static enum Foo {
        Bar, Baz
    }

    @Test
    public void testEnumValue() throws CustomParseException {
        MEnum<Foo> enumVal = new MValue.MEnum<Foo>(null, Foo.class.getName(), "Bar");
        Enum<Foo> obj = enumVal.asJavaObject();
        Assert.assertEquals(Foo.Bar, obj);
        try {
            new MValue.MEnum<Foo>(null, Foo.class.getName(), "Barr");
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
    }
    
    public void testNonEnumValue() throws CustomParseException {
        try {
            new MValue.MEnum<Foo>(null, Boolean.class.getName(), "TRUE");
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
    }

}

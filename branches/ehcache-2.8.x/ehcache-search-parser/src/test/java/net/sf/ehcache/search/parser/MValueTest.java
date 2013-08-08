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

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;

import net.sf.ehcache.search.parser.MValue.MEnum;
import net.sf.ehcache.search.parser.MValue.MObject;

import org.junit.Test;

public class MValueTest {

    public static enum Foo {
        Bar, Baz
    }

    ;

    @Test
    public void testEnumValue() throws CustomParseException {
        {
            MEnum enumVal = new MValue.MEnum(null, Foo.class.getName(), "Bar");
            Object obj = enumVal.asJavaObject();
            Assert.assertEquals(Foo.Bar, obj);
        }
        {
            try {
                @SuppressWarnings("unused")
                MEnum enumVal = new MValue.MEnum(null, Foo.class.getName(), "Barr");
                Assert.fail();
            } catch (Exception e) {
            }
        }

    }

    @Test
    public void testClassValue() throws CustomParseException {
        {
            MObject objValue = new MValue.MObject(null, String.class.getName(), "Bar");
            Object obj = objValue.asJavaObject();
            Assert.assertEquals("Bar", obj);
        }
        {
            try {
                @SuppressWarnings("unused")
                MObject objValue = new MValue.MObject(null, Integer.class.getName(), "Bar");
                Assert.fail();
            } catch (Exception e) {
            }
        }
    }

    @Test
    public void testHexString() throws CustomParseException {
        long seed = System.currentTimeMillis();
        System.out.println(this.getClass().getName() + ".testHexString() Random seed: " + seed);
        Random r = new Random(seed);
        byte[] seedArray = new byte[10];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seedArray.length; i++) {
            seedArray[i] = (byte)(r.nextInt() & 0xff);
            sb.append(String.format("%02x", seedArray[i]));
        }
        Assert.assertTrue(sb.length() == 2 * seedArray.length); // sanity
        MValue.MBinary binValue = new MValue.MBinary(null, sb.toString());
        Assert.assertTrue(Arrays.equals(seedArray, binValue.asJavaObject()));
    }

}

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

package net.sf.ehcache.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;


import org.junit.Test;

public class MergedEnumerationTest {

    @Test
    public void test() {
        List<String> list1 = new ArrayList<String>();
        list1.add("one");
        list1.add("two");
        List<String> list2 = new ArrayList<String>();
        list2.add("three");
        list2.add("four");
        Enumeration<String> enumeration1 =  Collections.enumeration(list1);
        Enumeration<String> enumeration2 =  Collections.enumeration(list2);

        MergedEnumeration mergedEnumeration = new MergedEnumeration(enumeration1,enumeration2);

        String[] mergedStrings = new String[4];
        int i =0;
        while(mergedEnumeration.hasMoreElements()){
            mergedStrings[i] = (String) mergedEnumeration.nextElement();
            i++;
        }
        assertEquals("one",mergedStrings[0]);
        assertEquals("two",mergedStrings[1]);
        assertEquals("three",mergedStrings[2]);
        assertEquals("four",mergedStrings[3]);

    }

}

/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

public class MemoryEfficientByteArrayOutputStreamTest {
    
    @Test
    public void testOutputIsCorrectlySized() throws IOException {
        Random rndm = new Random();
        
        for (int i = 0; i < 100; i++) {
            int size = rndm.nextInt(1024);
            int initial = rndm.nextInt(1024);
            MemoryEfficientByteArrayOutputStream out = new MemoryEfficientByteArrayOutputStream(initial);
            out.write(new byte[size]);
            Assert.assertEquals(size, out.getBytes().length);
        }
    }    
}

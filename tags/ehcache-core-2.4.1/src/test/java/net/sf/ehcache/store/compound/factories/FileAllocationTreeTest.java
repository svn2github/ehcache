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

package net.sf.ehcache.store.compound.factories;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import junit.framework.Assert;

public class FileAllocationTreeTest {

    @Test
    public void testUniformSizedAllocations() {
        FileAllocationTree test = new FileAllocationTree(100, null);

        for (int i = 0; i < 100; i++) {
            Assert.assertEquals(i, test.alloc(1).start());
        }
        Assert.assertEquals(100, test.getFileSize());
    }

    @Test
    public void testUniformSizedFrees() {
        FileAllocationTree test = new FileAllocationTree(100, null);
        test.alloc(100);
        Assert.assertEquals(100, test.getFileSize());

        for (int i = 0; i < 100; i++) {
            Assert.assertEquals(100, test.getFileSize());
            test.free(new Region(i));
        }
        Assert.assertEquals(0, test.getFileSize());
    }

    @Test
    public void testUniformRepeatedAllocFree() {
        FileAllocationTree test = new FileAllocationTree(100, null);

        for (int i = 1; i < 100; i++) {
            int count = (int) Math.floor(100d / i);
            for (int j = 1; j <= count; j++) {
                List<Region> regions = new ArrayList<Region>();
                for (int k = 0; k < j; k++) {
                    Region r = test.alloc(i);
                    Assert.assertEquals("Testing " + j + " Regions of size " + i + ": Alloc " + k, i, r.size());
                    Assert.assertEquals("Testing " + j + " Regions of size " + i + ": Alloc " + k, k * i, r.start());
                    regions.add(r);
                }
                for (Region r : regions) {
                    test.free(r);
                }
                Assert.assertEquals("Testing " + j + " Regions of size " + i, 0, test.getFileSize());
            }
        }
    }

    @Test
    public void testRandomAllocFree() {
        for (int n = 0; n < 100; n++) {
            FileAllocationTree test = new FileAllocationTree(10000, null);
            BitSet reference = new BitSet();
            Random rndm = new Random();

            for (int i = 0; i < 100; i++) {
                if (rndm.nextBoolean()) {
                    Region r = test.alloc(rndm.nextInt(100));
                    BitSet ref = reference.get((int) r.start(), (int) r.end() + 1);
                    reference.set((int) r.start(), (int) r.end() + 1);
                } else {
                    int length = reference.length();
                    if (length > 0) {
                        int random = rndm.nextInt(length);
                        int start = reference.nextSetBit(random);
                        if (start >= 0) {
                            int max = reference.nextClearBit(start);
                            int end = start + rndm.nextInt(max - start);
                            Region r = new Region(start, end);
                            test.free(r);
                            reference.clear(start, end + 1);
                        }
                    }
                }
            }
        }
    }
}

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

import static org.junit.Assert.*;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import net.sf.ehcache.statistics.extended.ExtendedStatisticsImpl;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome;

import org.junit.Test;

public class ExtendedStatisticsMBeanTest {

    public void testStupidEnums() {
        EnumSet<PutOutcome> test = EnumSet.allOf(StoreOperationOutcomes.PutOutcome.class);
        Assert.assertEquals(test.size(),StoreOperationOutcomes.PutOutcome.values().length);
        for(PutOutcome po:test) {
            System.out.println(po);
        }
        //System.out.println(ExtendedStatisticsImpl.ALL_STORE_PUT_OUTCOMES.size());
    }


    @Test
    public void testLongTime() throws InterruptedException {
        CacheManager cm = CacheManager.getInstance();
        final Cache cache = new Cache("test",400,null,false,null,true,120,120,false,1000,null);
        cm.addCache(cache);
        Thread t1=new Thread() {
            @Override
            public void run() {
                for(int i=0;i<100000;i++) {
                    cache.put(new Element(i,i+1));
                    try {
                        TimeUnit.MICROSECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                System.err.println("puts done");
            }
        };
        Thread t2=new Thread() {
            @Override
            public void run() {
                Random rand=new Random(0);
                for(int i=0;i<10000000;i++) {
                    cache.get(rand.nextInt(i+1));
                    try {
                        TimeUnit.MICROSECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                System.err.println("Gets done");
            }
        };
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        Thread.sleep(1000000);
    }

}

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
package net.sf.ehcache.hibernate.tm;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.xa.Xid;

/**
 * @author Alex Snaps
 */
public class SyncXid implements Xid {

    private static final int FORMAT_ID = 876543210;

    private static AtomicLong txIdCounter = new AtomicLong(Long.MIN_VALUE);
    private        byte[]     id          = longToBytes(txIdCounter.getAndIncrement());

    /**
     * {@inheritDoc}
     */
    public int getFormatId() {
        return FORMAT_ID;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getGlobalTransactionId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getBranchQualifier() {
        return new byte[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "SyncXid{" + "globalId=" + Arrays.toString(id) + '}';
    }

    private static byte[] longToBytes(long aLong) {
        byte[] array = new byte[8];

        array[7] = (byte)(aLong & 0xff);
        array[6] = (byte)((aLong >> 8) & 0xff);
        array[5] = (byte)((aLong >> 16) & 0xff);
        array[4] = (byte)((aLong >> 24) & 0xff);
        array[3] = (byte)((aLong >> 32) & 0xff);
        array[2] = (byte)((aLong >> 40) & 0xff);
        array[1] = (byte)((aLong >> 48) & 0xff);
        array[0] = (byte)((aLong >> 56) & 0xff);

        return array;
    }

}

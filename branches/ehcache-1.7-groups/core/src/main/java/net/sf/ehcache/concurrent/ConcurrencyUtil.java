/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.concurrent;

import net.sf.ehcache.CacheException;

/**
 * Various bits of black magic garnered from experts on the   concurrency-interest@cs.oswego.edu mailing list.
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class ConcurrencyUtil {

    private static final int DOUG_LEA_BLACK_MAGIC_OPERAND_1 = 20;
    private static final int DOUG_LEA_BLACK_MAGIC_OPERAND_2 = 12;
    private static final int DOUG_LEA_BLACK_MAGIC_OPERAND_3 = 7;
    private static final int DOUG_LEA_BLACK_MAGIC_OPERAND_4 = 4;


    /**
     * Utility class therefore precent construction
     */
    private ConcurrencyUtil() {
        //noop;
    }

    /**
     * Returns a hash code for non-null Object x.
     * <p/>
     * This function ensures that hashCodes that differ only by
     * constant multiples at each bit position have a bounded
     * number of collisions. (Doug Lea)
     *
     * @param object the object serving as a key
     * @return the hash code
     */
    public static int hash(Object object) {
        int h = object.hashCode();
        h ^= (h >>> DOUG_LEA_BLACK_MAGIC_OPERAND_1) ^ (h >>> DOUG_LEA_BLACK_MAGIC_OPERAND_2);
        return h ^ (h >>> DOUG_LEA_BLACK_MAGIC_OPERAND_3) ^ (h >>> DOUG_LEA_BLACK_MAGIC_OPERAND_4);
    }

    /**
     * Selects a lock for a key. The same lock is always used for a given key.
     *
     * @param key
     * @return the selected lock index
     */
    public static int selectLock(final Object key, int numberOfLocks) throws CacheException {
        int number = numberOfLocks & (numberOfLocks - 1);
        if (number != 0) {
            throw new CacheException("Lock number must be a power of two: " + numberOfLocks);
        }
        if (key == null) {
            return 0;
        } else {
            int hash = hash(key) & (numberOfLocks - 1);
            return hash;
        }

    }

}

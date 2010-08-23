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

/**
 * Memory size parser using the letter k or K to indicate kilobytes, the letter m or M to indicate megabytes,
 * the letter g or G to indicate gigabytes.
 * @author Ludovic Orban
 */
public class MemorySizeParser {

    private static final int MULTIPLICATION_FACTOR = 1024;

    /**
     * Parse a String containing a human-readable memory size.
     * @param memorySize the String containing a human-readable memory size.
     * @return the memory size in bytes.
     */
    public static long parse(String memorySize) {
        if (memorySize == null || "".equals(memorySize)) {
            return 0;
        }

        char unit = memorySize.charAt(memorySize.length() - 1);

        long multiplicationFactor;
        String memorySizeMinusUnit;

        switch (unit) {
            case 'k':
            case 'K':
                if (memorySize.length() < 2) {
                  throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                }
                multiplicationFactor = MULTIPLICATION_FACTOR;
                memorySizeMinusUnit = memorySize.substring(0, memorySize.length() - 1);
                break;
            case 'm':
            case 'M':
                if (memorySize.length() < 2) {
                    throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                }
                multiplicationFactor = MULTIPLICATION_FACTOR * MULTIPLICATION_FACTOR;
                memorySizeMinusUnit = memorySize.substring(0, memorySize.length() - 1);
                break;
            case 'g':
            case 'G':
                if (memorySize.length() < 2) {
                    throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                }
                multiplicationFactor = MULTIPLICATION_FACTOR * MULTIPLICATION_FACTOR * MULTIPLICATION_FACTOR;
                memorySizeMinusUnit = memorySize.substring(0, memorySize.length() - 1);
                break;
            default:
                try {
                    Integer.parseInt("" + unit);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                }
                multiplicationFactor = 1;
                memorySizeMinusUnit = memorySize;
        }

        try {
            long memorySizeLong = Long.parseLong(memorySizeMinusUnit);
            long result = memorySizeLong * multiplicationFactor;
            if (result < 0) {
                throw new IllegalArgumentException("memory size cannot be negative [" + memorySize + "]");
            }
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
        }
    }

}

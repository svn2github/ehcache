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
 *
 * @author Ludovic Orban
 */
public class MemorySizeParser {
    private static final long BYTE = 1;
    private static final long KILOBYTE = 1024;
    private static final long MEGABYTE = 1024 * KILOBYTE;
    private static final long GIGABYTE = 1024 * MEGABYTE;

    /**
     * Parse a String containing a human-readable memory size.
     *
     * @param memorySize the String containing a human-readable memory size.
     * @return the memory size in bytes.
     */
    public static long parse(String memorySize) {
        if (memorySize == null || "".equals(memorySize)) {
            return 0;
        }

        MemorySize size = toUnit(memorySize);
        return size.calculateMemorySizeInBytes();
    }

    private static MemorySize toUnit(String memorySize) {
        char unit = memorySize.charAt(memorySize.length() - 1);

        switch (unit) {
            case 'k':
            case 'K':
                if (memorySize.length() < 2) {
                    throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                }
                return new MemorySize(memorySize.substring(0, memorySize.length() - 1), KILOBYTE);
            case 'm':
            case 'M':
                if (memorySize.length() < 2) {
                    throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                }
                return new MemorySize(memorySize.substring(0, memorySize.length() - 1), MEGABYTE);
            case 'g':
            case 'G':
                if (memorySize.length() < 2) {
                    throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                }
                return new MemorySize(memorySize.substring(0, memorySize.length() - 1), GIGABYTE);
            default:
                try {
                    Integer.parseInt("" + unit);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                }
                return new MemorySize(memorySize, BYTE);
        }
    }

    /**
     * Memory size calculator.
     */
    private static final class MemorySize {
        private long multiplicationFactor;
        private String memorySizeString;

        private MemorySize(String memorySizeString, long multiplicationFactor) {
            this.multiplicationFactor = multiplicationFactor;
            this.memorySizeString = memorySizeString;
        }

        public long calculateMemorySizeInBytes() {
            try {
                long memorySizeLong = Long.parseLong(memorySizeString);
                long result = memorySizeLong * multiplicationFactor;
                if (result < 0) {
                    throw new IllegalArgumentException("memory size cannot be negative");
                }
                return result;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid format for memory size");
            }
        }
    }

}

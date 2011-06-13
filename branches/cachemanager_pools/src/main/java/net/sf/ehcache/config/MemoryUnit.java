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

package net.sf.ehcache.config;

/**
 * @author Alex Snaps
 */
public enum MemoryUnit {

    BYTES(0, 'b') {
        public long toBytes(final long amount) { return amount; }
        public long toKiloBytes(final long amount) { return safeShift(amount, KILOBYTES.offset - BYTES.offset); }
        public long toMegaBytes(final long amount) { return safeShift(amount, MEGABYTES.offset - BYTES.offset); }
        public long toGigaBytes(final long amount) { return safeShift(amount, GIGABYTES.offset - BYTES.offset); }
    },
    KILOBYTES(BYTES.offset + 10, 'k') {
        public long toBytes(final long amount) { return safeShift(amount, BYTES.offset - KILOBYTES.offset); }
        public long toKiloBytes(final long amount) { return amount; }
        public long toMegaBytes(final long amount) { return safeShift(amount, MEGABYTES.offset - KILOBYTES.offset); }
        public long toGigaBytes(final long amount) { return safeShift(amount, GIGABYTES.offset - KILOBYTES.offset); }
    },
    MEGABYTES(KILOBYTES.offset + 10, 'm') {
        public long toBytes(final long amount) { return safeShift(amount, BYTES.offset - MEGABYTES.offset); }
        public long toKiloBytes(final long amount) { return safeShift(amount, KILOBYTES.offset - MEGABYTES.offset); }
        public long toMegaBytes(final long amount) { return amount; }
        public long toGigaBytes(final long amount) { return safeShift(amount, GIGABYTES.offset - MEGABYTES.offset); }
    },
    GIGABYTES(MEGABYTES.offset + 10, 'g') {
        public long toBytes(final long amount) { return safeShift(amount, BYTES.offset - GIGABYTES.offset); }
        public long toKiloBytes(final long amount) { return safeShift(amount, KILOBYTES.offset - GIGABYTES.offset); }
        public long toMegaBytes(final long amount) { return safeShift(amount, MEGABYTES.offset - GIGABYTES.offset); }
        public long toGigaBytes(final long amount) { return amount; }
    };

    private static long safeShift(final long unit, final long shift) {
        if (shift > 0) {
            return unit >>> shift;
        } else if (shift <= -1 * Long.numberOfLeadingZeros(unit)) {
          return Long.MAX_VALUE;
        } else {
            return unit << -shift;
        }
    }

    private        final int offset;
    private        final char unit;

    MemoryUnit(final int offset, final char unit) {
        this.offset = offset;
        this.unit = unit;
    }

    public char getUnit() {
        return unit;
    }

    public abstract long toBytes(long amount);
    public abstract long toKiloBytes(long amount);
    public abstract long toMegaBytes(long amount);
    public abstract long toGigaBytes(long amount);

    public String toString(final long amount) {
        return amount + Character.toString(this.unit);
    }

    public static MemoryUnit forUnit(final char unit) {
        for (MemoryUnit memoryUnit : values()) {
            if (memoryUnit.unit == unit) {
                return memoryUnit;
            }
        }
        throw new IllegalArgumentException("'" + unit + "' suffix doesn't match any SizeUnit");
    }

    public static MemoryUnit parseUnit(final String value) {
        if (hasUnit(value)) {
            return forUnit(Character.toLowerCase(value.charAt(value.length() - 1)));
        }
        return BYTES;
    }

    public static long parseAmount(final String value) throws NumberFormatException {
        if (value == null) {
            throw new NullPointerException("Value can't be null!");
        }

        if (value.length() == 0) {
            throw new IllegalArgumentException("Value can't be an empty string!");
        }

        if (hasUnit(value)) {
            return Long.parseLong(value.substring(0, value.length() - 1).trim());
        } else {
            return Long.parseLong(value);
        }
    }

    public static long parseSizeInBytes(final String value) throws NumberFormatException, IllegalArgumentException {
        if (value.length() == 0) {
            throw new IllegalArgumentException("Value can't be an empty string!");
        }

        MemoryUnit memoryUnit = parseUnit(value);
        return memoryUnit.toBytes(parseAmount(value));
    }

    private static boolean hasUnit(final String value) {
        if (value.length() > 0) {
            char potentialUnit = value.charAt(value.length() - 1);
            return potentialUnit < '0' || potentialUnit > '9';
        }
        return false;
    }
}

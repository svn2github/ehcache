package net.sf.ehcache.util;

/**
 * Memory size parser using the letter k or K to indicate kilobytes, the letter m or M to indicate megabytes,
 * the letter g or G to indicate gigabytes.
 * @author Ludovic Orban
 */
public class MemorySizeParser {

    /**
     * Parse a String containing a human-readable memory size.
     * @param memorySize the String containing a human-readable memory size.
     * @return the memory size in bytes.
     */
    public static long parse(String memorySize) {
        if (memorySize == null || "".equals(memorySize))
            return 0;

        char unit = memorySize.charAt(memorySize.length() -1);

        long multiplicationFactor;
        String memorySizeMinusUnit;

        switch (unit) {
            case 'k':
            case 'K':
                if (memorySize.length() < 2)
                    throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                multiplicationFactor = 1024;
                memorySizeMinusUnit = memorySize.substring(0, memorySize.length() -1);
                break;
            case 'm':
            case 'M':
                if (memorySize.length() < 2)
                    throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                multiplicationFactor = 1024 * 1024;
                memorySizeMinusUnit = memorySize.substring(0, memorySize.length() -1);
                break;
            case 'g':
            case 'G':
                if (memorySize.length() < 2)
                    throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
                multiplicationFactor = 1024 * 1024 * 1024;
                memorySizeMinusUnit = memorySize.substring(0, memorySize.length() -1);
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
            if (result < 0)
                throw new IllegalArgumentException("memory size cannot be negative [" + memorySize + "]");
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid format for memory size [" + memorySize + "]");
        }
    }

}

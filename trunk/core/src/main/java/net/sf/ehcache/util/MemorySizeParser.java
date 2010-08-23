package net.sf.ehcache.util;

/**
 * @author lorban
 */
public class MemorySizeParser {

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

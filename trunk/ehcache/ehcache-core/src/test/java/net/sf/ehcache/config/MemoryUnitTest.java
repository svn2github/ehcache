package net.sf.ehcache.config;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class MemoryUnitTest {

    @Test
    public void testOneUnitConversions() {
        assertThat(MemoryUnit.BYTES.toBytes(1), equalTo(1L));
        assertThat(MemoryUnit.BYTES.toKiloBytes(1024), equalTo(1L));
        assertThat(MemoryUnit.BYTES.toMegaBytes(1024 * 1024), equalTo(1L));
        assertThat(MemoryUnit.BYTES.toGigaBytes(1024 * 1024 * 1024), equalTo(1L));

        assertThat(MemoryUnit.KILOBYTES.toBytes(1), equalTo(1024L));
        assertThat(MemoryUnit.KILOBYTES.toKiloBytes(1), equalTo(1L));
        assertThat(MemoryUnit.KILOBYTES.toMegaBytes(1024), equalTo(1L));
        assertThat(MemoryUnit.KILOBYTES.toGigaBytes(1024 * 1024), equalTo(1L));

        assertThat(MemoryUnit.MEGABYTES.toBytes(1), equalTo((long) 1024 * 1024));
        assertThat(MemoryUnit.MEGABYTES.toKiloBytes(1), equalTo((long) 1024) );
        assertThat(MemoryUnit.MEGABYTES.toMegaBytes(1), equalTo(1L) );
        assertThat(MemoryUnit.MEGABYTES.toGigaBytes(1024), equalTo(1L) );

        assertThat(MemoryUnit.GIGABYTES.toBytes(1), equalTo((long) 1024 * 1024 * 1024));
        assertThat(MemoryUnit.GIGABYTES.toKiloBytes(1), equalTo((long) 1024 * 1024));
        assertThat(MemoryUnit.GIGABYTES.toMegaBytes(1), equalTo((long) 1024));
        assertThat(MemoryUnit.GIGABYTES.toGigaBytes(1), equalTo(1L));
    }

    @Test
    public void testZeroUnitConversion() {
        assertThat(MemoryUnit.BYTES.toBytes(0), equalTo(0L));
        assertThat(MemoryUnit.BYTES.toKiloBytes(0), equalTo(0L));
        assertThat(MemoryUnit.BYTES.toMegaBytes(0), equalTo(0L));
        assertThat(MemoryUnit.BYTES.toGigaBytes(0), equalTo(0L));
        assertThat(MemoryUnit.BYTES.toKiloBytes(1), equalTo(0L));
        assertThat(MemoryUnit.BYTES.toKiloBytes(512), equalTo(0L));
        assertThat(MemoryUnit.BYTES.toKiloBytes(1023), equalTo(0L));
        assertThat(MemoryUnit.BYTES.toMegaBytes(1024*512), equalTo(0L));
        assertThat(MemoryUnit.BYTES.toGigaBytes(1024*1024*512), equalTo(0L));

        assertThat(MemoryUnit.KILOBYTES.toBytes(0), equalTo(0L));
        assertThat(MemoryUnit.KILOBYTES.toKiloBytes(0), equalTo(0L));
        assertThat(MemoryUnit.KILOBYTES.toMegaBytes(0), equalTo(0L));
        assertThat(MemoryUnit.KILOBYTES.toGigaBytes(0), equalTo(0L));
        assertThat(MemoryUnit.KILOBYTES.toKiloBytes(512), equalTo(512L));
        assertThat(MemoryUnit.KILOBYTES.toMegaBytes(1), equalTo(0L));
        assertThat(MemoryUnit.KILOBYTES.toMegaBytes(512), equalTo(0L));
        assertThat(MemoryUnit.KILOBYTES.toMegaBytes(1023), equalTo(0L));
        assertThat(MemoryUnit.KILOBYTES.toGigaBytes(1024*512), equalTo(0L));

        assertThat(MemoryUnit.MEGABYTES.toBytes(0), equalTo(0L));
        assertThat(MemoryUnit.MEGABYTES.toKiloBytes(0), equalTo(0L));
        assertThat(MemoryUnit.MEGABYTES.toMegaBytes(0), equalTo(0L) );
        assertThat(MemoryUnit.MEGABYTES.toGigaBytes(0), equalTo(0L) );
        assertThat(MemoryUnit.MEGABYTES.toMegaBytes(512), equalTo(512L) );
        assertThat(MemoryUnit.MEGABYTES.toGigaBytes(1), equalTo(0L) );
        assertThat(MemoryUnit.MEGABYTES.toGigaBytes(512), equalTo(0L) );
        assertThat(MemoryUnit.MEGABYTES.toGigaBytes(1023), equalTo(0L) );

        assertThat(MemoryUnit.GIGABYTES.toBytes(0), equalTo(0L));
        assertThat(MemoryUnit.GIGABYTES.toKiloBytes(0), equalTo(0L));
        assertThat(MemoryUnit.GIGABYTES.toMegaBytes(0), equalTo(0L));
        assertThat(MemoryUnit.GIGABYTES.toGigaBytes(0), equalTo(0L));
    }

    @Test
    public void testLargeConversion() {
        assertThat(MemoryUnit.BYTES.toBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE));
        assertThat(MemoryUnit.BYTES.toKiloBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE / 1024));
        assertThat(MemoryUnit.BYTES.toMegaBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE / 1024 / 1024));
        assertThat(MemoryUnit.BYTES.toGigaBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE / 1024 / 1024 / 1024));

        assertThat(MemoryUnit.KILOBYTES.toBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE));
        assertThat(MemoryUnit.KILOBYTES.toKiloBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE));
        assertThat(MemoryUnit.KILOBYTES.toMegaBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE / 1024));
        assertThat(MemoryUnit.KILOBYTES.toGigaBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE / 1024 / 1024));

        assertThat(MemoryUnit.MEGABYTES.toBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE));
        assertThat(MemoryUnit.MEGABYTES.toKiloBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE));
        assertThat(MemoryUnit.MEGABYTES.toMegaBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE) );
        assertThat(MemoryUnit.MEGABYTES.toGigaBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE / 1024) );

        assertThat(MemoryUnit.GIGABYTES.toBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE));
        assertThat(MemoryUnit.GIGABYTES.toKiloBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE));
        assertThat(MemoryUnit.GIGABYTES.toMegaBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE));
        assertThat(MemoryUnit.GIGABYTES.toGigaBytes(Long.MAX_VALUE), equalTo(Long.MAX_VALUE));
    }

    @Test
    public void testParsing() {
        assertThat(MemoryUnit.parseUnit("100"), is(MemoryUnit.BYTES));
        assertThat(MemoryUnit.parseUnit("100b"), is(MemoryUnit.BYTES));
        assertThat(MemoryUnit.parseUnit("100 B"), is(MemoryUnit.BYTES));
        assertThat(MemoryUnit.parseUnit("100 k"), is(MemoryUnit.KILOBYTES));
        assertThat(MemoryUnit.parseUnit("100K"), is(MemoryUnit.KILOBYTES));
        assertThat(MemoryUnit.parseUnit("100M"), is(MemoryUnit.MEGABYTES));
        assertThat(MemoryUnit.parseUnit("0G"), is(MemoryUnit.GIGABYTES));

        assertThat(MemoryUnit.parseAmount("100"), is(100L));
        assertThat(MemoryUnit.parseAmount("100B"), is(100L));
        assertThat(MemoryUnit.parseAmount("0M"), is(0L));
        assertThat(MemoryUnit.parseAmount(Long.MAX_VALUE + "k"), is(Long.MAX_VALUE));

        assertThat(MemoryUnit.parseSizeInBytes("2M"), is((long) 2 * 1024 * 1024));
        assertThat(MemoryUnit.parseSizeInBytes("0M"), is((long) 0));
        assertThat(MemoryUnit.parseSizeInBytes("0G"), is((long) 0));
        assertThat(MemoryUnit.parseSizeInBytes("24G"), is((long) 24 * 1024 * 1024 * 1024));
        assertThat(MemoryUnit.parseSizeInBytes("24 G"), is((long) 24 * 1024 * 1024 * 1024));
        assertThat(MemoryUnit.parseSizeInBytes(Long.MAX_VALUE + "k"), is(Long.MAX_VALUE));
    }

    @Test
    public void testIllegalValues() {
        try {
            MemoryUnit.parseSizeInBytes("1.5G");
            fail("This should have thrown an NumberFormatException");
        } catch (NumberFormatException e) {
            // Expected!
        }

        try {
            MemoryUnit.parseSizeInBytes("15L");
            fail("This should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected!
        }

        try {
            MemoryUnit.parseSizeInBytes("");
            fail("This should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected!
        }

        try {
            MemoryUnit.parseSizeInBytes("G");
            fail("This should have thrown an NumberFormatException");
        } catch (NumberFormatException e) {
            // Excepted!
        }

    }

}

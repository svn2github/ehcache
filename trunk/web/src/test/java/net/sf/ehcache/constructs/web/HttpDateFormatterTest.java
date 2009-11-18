package net.sf.ehcache.constructs.web;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.Date;

/**
 * Unit test for HttpDateFormatter
 * @author Greg Luck
 */
public class HttpDateFormatterTest {

    /**
     * Checks we get the right format
     */
    @Test
    public void testDateFromString() {

        HttpDateFormatter httpDateFormatter = new HttpDateFormatter();

        String dateString1 = "Sun, 06 Nov 1994 08:49:37 GMT";
        Date date = httpDateFormatter.parseDateFromHttpDate(dateString1);
        String dateString2 = httpDateFormatter.formatHttpDate(date);
        assertEquals(dateString1, dateString2);
    }

    /**
     * Checks we do not throw a ParseException, and that we return 1/1/1970, the start of the Unix era.
     */
    @Test
    public void testParseBadDate() {

        HttpDateFormatter httpDateFormatter = new HttpDateFormatter();

        String dateString1 = "dddddddddd^^^3s";
        Date date = httpDateFormatter.parseDateFromHttpDate(dateString1);
        assertEquals(new Date(0), date);
    }



}

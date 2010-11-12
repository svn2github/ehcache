package net.sf.ehcache.writer.writebehind.operations;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.Element;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class DeleteOperationTest {

    private static final String OUR_KEY = "ourKey";
    private static final String OTHER_KEY = "otherKey";

    @Test
    public void testEquals() throws Exception {

        DeleteOperation op1 = new DeleteOperation(new CacheEntry(OUR_KEY, new Element(OUR_KEY, "someValue")));
        DeleteOperation op2 = new DeleteOperation(new CacheEntry(OUR_KEY, new Element(OUR_KEY, "someOtherValue")));
        DeleteOperation op3 = new DeleteOperation(new CacheEntry(OTHER_KEY, new Element(OTHER_KEY, "someOtherValue")));

        assertThat("Two delete operations for the same key are to be considered equal", op1.equals(op2), is(true));
        assertThat("Two delete operations for the different keys are not to be equal", op1.equals(op3), is(false));
    }

    @Test
    public void testHashCode() throws Exception {
        DeleteOperation op1 = new DeleteOperation(new CacheEntry(OUR_KEY, new Element(OUR_KEY, "someValue")));
        DeleteOperation op2 = new DeleteOperation(new CacheEntry(OUR_KEY, new Element(OUR_KEY, "someOtherValue")));

        assertThat("A Delete operation should have the same hashCode as its key", op1.hashCode(), is(OUR_KEY.hashCode()));
        assertThat("Delete operations for the same key, should have the same hashCode", op1.hashCode(), is(op2.hashCode()));
    }
}

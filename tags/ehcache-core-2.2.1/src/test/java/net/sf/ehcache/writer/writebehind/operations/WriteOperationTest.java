package net.sf.ehcache.writer.writebehind.operations;

import net.sf.ehcache.Element;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class WriteOperationTest {

    private static final Object OUR_KEY = "ourKey";

    @Test
    public void testEquals() throws Exception {
        WriteOperation op1 = new WriteOperation(new Element(OUR_KEY, "someValue"));
        WriteOperation op2 = new WriteOperation(new Element(OUR_KEY, "someOtherValue"));
        WriteOperation op3 = new WriteOperation(new Element(OUR_KEY, "someValue"));
        WriteOperation op4 = new WriteOperation(new Element("otherKey", "someValue"));

        assertThat("Two write operations for the same key and same value should be equal",
            op1.equals(op3), is(true));
        assertThat("Two write operations for the same key, but with different values should be different",
            op1.equals(op2), is(false));
        assertThat("Two write operations for the different keys should be different",
            op1.equals(op4), is(false));
    }

    @Test
    public void testHashCode() throws Exception {
        WriteOperation op1 = new WriteOperation(new Element(OUR_KEY, "someValue"));
        WriteOperation op2 = new WriteOperation(new Element(OUR_KEY, "someOtherValue"));
        WriteOperation op3 = new WriteOperation(new Element(OUR_KEY, "someValue"));

        assertThat("A write operation should have the same hashCode as its key", op1.hashCode(), is(OUR_KEY.hashCode()));
        assertThat("Two write operations for the same key but different values should still have the same hashCode",
            op1.hashCode(), is(op2.hashCode()));
        assertThat("Two write operations for the same key and same value should have the same hashCode",
            op2.hashCode(), is(op3.hashCode()));
    }
}

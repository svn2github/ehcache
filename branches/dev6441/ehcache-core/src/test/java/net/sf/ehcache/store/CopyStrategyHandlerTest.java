package net.sf.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * CopyStrategyHandlerTest
 */
public class CopyStrategyHandlerTest {

    private ReadWriteCopyStrategy<Element> copyStrategy;
    private ClassLoader loader = getClass().getClassLoader();

    @Before
    public void setUp() {
        copyStrategy = mock(ReadWriteCopyStrategy.class);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(copyStrategy);
    }

    @Test
    public void given_no_copy_with_no_copy_strategy_when_constructing_then_valid() {
        new CopyStrategyHandler(false, false, null, loader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_copy_on_read_with_no_copy_strategy_when_constructing_then_exception() {
        new CopyStrategyHandler(true, false, null, loader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_copy_on_write_with_no_copy_strategy_when_constructing_then_exception() {
        new CopyStrategyHandler(true, false, null, loader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_copy_on_read_and_write_with_no_copy_strategy_when_constructing_then_exception() {
        new CopyStrategyHandler(true, true, null, loader);
    }

    @Test
    public void given_no_copy_when_isCopyActive_then_false() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, false, null, loader);
        assertThat(copyStrategyHandler.isCopyActive(), is(false));
    }

    @Test
    public void given_copy_on_read_when_isCopyActive_then_false() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, false, copyStrategy, loader);
        assertThat(copyStrategyHandler.isCopyActive(), is(true));
    }

    @Test
    public void given_copy_on_write_when_isCopyActive_then_false() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, false, copyStrategy, loader);
        assertThat(copyStrategyHandler.isCopyActive(), is(true));
    }

    @Test
    public void given_copy_on_read_and_write_when_isCopyActive_then_false() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, false, copyStrategy, loader);
        assertThat(copyStrategyHandler.isCopyActive(), is(true));
    }

    @Test
    public void given_no_copy_when_copyElementForReadIfNeeded_with_Element_then_returns_same() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, false, null, loader);
        Element element = new Element("key", "value");
        assertThat(copyStrategyHandler.copyElementForReadIfNeeded(element), sameInstance(element));
    }

    @Test
    public void given_no_copy_when_copyElementForReadIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, false, null, loader);
        assertThat(copyStrategyHandler.copyElementForReadIfNeeded(null), nullValue());
    }

    @Test
    public void given_no_copy_when_copyElementForWriteIfNeeded_with_Element_then_returns_same() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, false, null, loader);
        Element element = new Element("key", "value");
        assertThat(copyStrategyHandler.copyElementForWriteIfNeeded(element), sameInstance(element));
    }

    @Test
    public void given_no_copy_when_copyElementForWriteIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, false, null, loader);
        assertThat(copyStrategyHandler.copyElementForWriteIfNeeded(null), nullValue());
    }

    @Test
    public void given_no_copy_when_copyElementForRemovalIfNeeded_with_Element_then_returns_same() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, false, null, loader);
        Element element = new Element("key", "value");
        assertThat(copyStrategyHandler.copyElementForRemovalIfNeeded(element), sameInstance(element));
    }

    @Test
    public void given_no_copy_when_copyElementForRemovalIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, false, null, loader);
        assertThat(copyStrategyHandler.copyElementForRemovalIfNeeded(null), nullValue());
    }

    @Test
    public void given_copy_on_read_when_copyElementForReadIfNeeded_with_Element_then_returns_different() {
        Element element = new Element("key", "value");
        Element serial = new Element("key", new byte[] { });
        when(copyStrategy.copyForWrite(element, loader)).thenReturn(serial);
        when(copyStrategy.copyForRead(serial, loader)).thenReturn(new Element("key", "value"));
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, false, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForReadIfNeeded(element), allOf(not(sameInstance(element)), is(element)));
        verify(copyStrategy).copyForWrite(element, loader);
        verify(copyStrategy).copyForRead(serial, loader);
    }

    @Test
    public void given_copy_on_read_when_copyElementForReadIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, false, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForReadIfNeeded(null), nullValue());
    }

    @Test
    public void given_copy_on_read_when_copyElementForWriteIfNeeded_with_Element_then_returns_same() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, false, copyStrategy, loader);
        Element element = new Element("key", "value");
        assertThat(copyStrategyHandler.copyElementForWriteIfNeeded(element), sameInstance(element));
    }

    @Test
    public void given_copy_on_read_when_copyElementForWriteIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, false, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForWriteIfNeeded(null), nullValue());
    }

    @Test
    public void given_copy_on_read_when_copyElementForRemovalIfNeeded_with_Element_then_returns_same() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, false, copyStrategy, loader);
        Element element = new Element("key", "value");
        assertThat(copyStrategyHandler.copyElementForRemovalIfNeeded(element), sameInstance(element));
    }

    @Test
    public void given_copy_on_read_when_copyElementForRemovalIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, false, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForRemovalIfNeeded(null), nullValue());
    }

    @Test
    public void given_copy_on_write_when_copyElementForReadIfNeeded_with_Element_then_returns_same() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, true, copyStrategy, loader);
        Element element = new Element("key", "value");
        assertThat(copyStrategyHandler.copyElementForReadIfNeeded(element), sameInstance(element));
    }

    @Test
    public void given_copy_on_write_when_copyElementForReadIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, true, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForReadIfNeeded(null), nullValue());
    }

    @Test
    public void given_copy_on_write_when_copyElementForWriteIfNeeded_with_Element_then_returns_different() {
        Element element = new Element("key", "value");
        Element serial = new Element("key", new byte[] { });
        when(copyStrategy.copyForWrite(element, loader)).thenReturn(serial);
        when(copyStrategy.copyForRead(serial, loader)).thenReturn(new Element("key", "value"));
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, true, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForWriteIfNeeded(element), allOf(not(sameInstance(element)), equalTo(element)));
        verify(copyStrategy).copyForWrite(element, loader);
        verify(copyStrategy).copyForRead(serial, loader);
    }

    @Test
    public void given_copy_on_write_when_copyElementForWriteIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, true, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForWriteIfNeeded(null), nullValue());
    }

    @Test
    public void given_copy_on_write_when_copyElementForRemovalIfNeeded_with_Element_then_returns_same() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, true, copyStrategy, loader);
        Element element = new Element("key", "value");
        assertThat(copyStrategyHandler.copyElementForRemovalIfNeeded(element), sameInstance(element));
    }

    @Test
    public void given_copy_on_write_when_copyElementForRemovalIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(false, true, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForRemovalIfNeeded(null), nullValue());
    }

    @Test
    public void given_copy_on_read_and_write_when_copyElementForReadIfNeeded_with_Element_then_returns_different() {
        Element element = new Element("key", "value");
        Element serial = new Element("key", new byte[] { });
        when(copyStrategy.copyForRead(serial, loader)).thenReturn(new Element("key", "value"));
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, true, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForReadIfNeeded(serial), is(element));
        verify(copyStrategy).copyForRead(serial, loader);
    }

    @Test
    public void given_copy_on_read_and_write_when_copyElementForReadIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, true, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForReadIfNeeded(null), nullValue());
    }

    @Test
    public void given_copy_on_read_and_write_when_copyElementForWriteIfNeeded_with_Element_then_returns_different() {
        Element element = new Element("key", "value");
        Element serial = new Element("key", new byte[] { });
        when(copyStrategy.copyForWrite(element, loader)).thenReturn(new Element("key", new byte[] { }));
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, true, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForWriteIfNeeded(element), is(serial));
        verify(copyStrategy).copyForWrite(element, loader);
    }

    @Test
    public void given_copy_on_read_and_write_when_copyElementForWriteIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, true, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForWriteIfNeeded(null), nullValue());
    }

    @Test
    public void given_copy_on_read_and_write_when_copyElementForRemovalIfNeeded_with_Element_then_returns_different() {
        Element element = new Element("key", "value");
        Element serial = new Element("key", new byte[] { });
        when(copyStrategy.copyForWrite(element, loader)).thenReturn(new Element("key", new byte[] { }));
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, true, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForRemovalIfNeeded(element), is(serial));
        verify(copyStrategy).copyForWrite(element, loader);
    }

    @Test
    public void given_copy_on_read_and_write_when_copyElementForRemovalIfNeeded_with_null_then_returns_null() {
        CopyStrategyHandler copyStrategyHandler = new CopyStrategyHandler(true, true, copyStrategy, loader);
        assertThat(copyStrategyHandler.copyElementForRemovalIfNeeded(null), nullValue());
    }

}

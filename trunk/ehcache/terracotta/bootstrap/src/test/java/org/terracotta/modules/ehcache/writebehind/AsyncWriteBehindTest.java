/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.CoalesceKeysFilter;
import net.sf.ehcache.writer.writebehind.OperationsFilter;
import net.sf.ehcache.writer.writebehind.operations.KeyBasedOperation;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.terracotta.modules.ehcache.async.AsyncCoordinator;
import org.terracotta.modules.ehcache.async.scatterpolicies.ItemScatterPolicy;
import org.terracotta.modules.ehcache.writebehind.operations.DeleteAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.SingleAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.WriteAsyncOperation;
import org.terracotta.test.categories.CheckShorts;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Category(CheckShorts.class)
public class AsyncWriteBehindTest {

  public static final int CONCURRENCY = 10;

  private AsyncCoordinator<SingleAsyncOperation> asyncCoordinator;
  private AsyncWriteBehind asyncWriteBehind;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() {
    asyncCoordinator = mock(AsyncCoordinator.class);
    asyncWriteBehind = new AsyncWriteBehind(asyncCoordinator, CONCURRENCY);
  }


  @Test(expected = IllegalArgumentException.class)
  public void testConstructorRequiresNonNullAsyncCoordinator() {
    new AsyncWriteBehind(null, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorRequiresNonNegativeConcurrency() {
    new AsyncWriteBehind(asyncCoordinator, -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorRequiresNonZeroConcurrency() {
    new AsyncWriteBehind(asyncCoordinator, 0);
  }

  @Test
  public void testConstructorAcceptsAConcurrencyOfOne() {
    new AsyncWriteBehind(asyncCoordinator, 1);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPassesConcurrencyToAsyncCoordinatorOnStart() {
    asyncWriteBehind.start(null);
    verify(asyncCoordinator).start(any(CacheWriterProcessor.class), eq(CONCURRENCY), any(ItemScatterPolicy.class));
  }

  @Test
  public void testPassesItemScatterPolicyToAsyncCoordinatorOnStart() {
    asyncWriteBehind.start(null);
    verify(asyncCoordinator).start(any(CacheWriterProcessor.class), eq(CONCURRENCY), same(AsyncWriteBehind.POLICY));
  }

  @Test
  public void testPassesWrappedWriterToAsyncCoordinatorOnStart() {
    final CacheWriter writer = mock(CacheWriter.class);
    asyncWriteBehind.start(writer);
    ArgumentCaptor<CacheWriterProcessor> captor = ArgumentCaptor.forClass(CacheWriterProcessor.class);
    verify(asyncCoordinator).start(captor.capture(), eq(CONCURRENCY), same(AsyncWriteBehind.POLICY));
    assertThat(captor.getAllValues().size(), is(1));
    assertThat(captor.getValue().getCacheWriter(), sameInstance(writer));
  }

  @Test
  public void testDelegatesDeleteToAsyncCoordinator() {
    final Element element = new Element("foo", "bar");
    asyncWriteBehind.delete(new CacheEntry(element.getObjectKey(), element));
    ArgumentCaptor<SingleAsyncOperation> captor = ArgumentCaptor.forClass(SingleAsyncOperation.class);
    verify(asyncCoordinator).add(captor.capture());
    assertThat(captor.getAllValues().size(), is(1));
    final SingleAsyncOperation value = captor.getValue();
    assertThat(value, instanceOf(DeleteAsyncOperation.class));
    assertThat(value.getKey(), sameInstance(element.getObjectKey()));
    assertThat(value.getElement(), sameInstance(element));
  }

  @Test
  public void testDelegatesWriteToAsyncCoordinator() {
    final Element element = new Element("foo", "bar");
    asyncWriteBehind.write(element);
    ArgumentCaptor<SingleAsyncOperation> captor = ArgumentCaptor.forClass(SingleAsyncOperation.class);
    verify(asyncCoordinator).add(captor.capture());
    assertThat(captor.getAllValues().size(), is(1));
    final SingleAsyncOperation value = captor.getValue();
    assertThat(value, instanceOf(WriteAsyncOperation.class));
    assertThat(value.getElement(), sameInstance(element));
  }

  @Test
  public void testDelegatesStopToAsyncCoordinator() {
    asyncWriteBehind.stop();
    verify(asyncCoordinator).stop();
  }

  @Test
  public void testDelegatesGetQueueSizeToAsyncCoordinator() {
    asyncWriteBehind.getQueueSize();
    verify(asyncCoordinator).getQueueSize();
  }

  @Test
  public void testDelegatesSetOperationsFilterToAsyncCoordinator() {
    final CoalesceKeysFilter filter = new CoalesceKeysFilter();
    asyncWriteBehind.setOperationsFilter(filter);
    ArgumentCaptor<OperationsFilterWrapper> captor = ArgumentCaptor.forClass(OperationsFilterWrapper.class);
    verify(asyncCoordinator).setOperationsFilter(captor.capture());
    assertThat(captor.getAllValues().size(), is(1));
    assertThat(captor.getValue().getDelegate(), CoreMatchers.<OperationsFilter<KeyBasedOperation>>sameInstance(filter));
  }

  @Test
  public void testDefaultScatterMethod() {
    assertThat(AsyncWriteBehind.POLICY.getClass().getName(), is(AsyncWriteBehind.class.getName() + "$SingleAsyncOperationItemScatterPolicy"));
    for(int i = -100; i < 100; i++) {
      final SingleAsyncOperation mock = mock(SingleAsyncOperation.class);
      when(mock.getKey()).thenReturn(i);
      final int actual = AsyncWriteBehind.POLICY.selectBucket(CONCURRENCY, mock);
      assertTrue(actual >= 0);
      assertTrue(actual < CONCURRENCY);
    }
  }
}

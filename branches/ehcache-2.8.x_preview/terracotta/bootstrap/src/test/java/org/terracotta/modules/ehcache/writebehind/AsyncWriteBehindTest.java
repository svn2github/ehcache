/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.writer.CacheWriter;

import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.modules.ehcache.async.AsyncCoordinator;

// TODO: Write a system test for these ops and remove this unit test
@Ignore
public class AsyncWriteBehindTest {

  @Test(expected = IllegalStateException.class)
  public void testDeleteThrowsWhenNotStarted() {
    AsyncWriteBehind writeBehind = createAsyncWriteBehind();
    writeBehind.delete(new CacheEntry("", new Element("", "")));
  }

  @Test(expected = IllegalStateException.class)
  public void testDeleteThrowsWhenNotStopped() {
    AsyncWriteBehind writeBehind = createAsyncWriteBehind();
    writeBehind.start(mock(CacheWriter.class));
    writeBehind.stop();
    writeBehind.delete(new CacheEntry("", new Element("", "")));
  }

  @Test(expected = IllegalStateException.class)
  public void testWriteThrowsWhenNotStarted() {
    AsyncWriteBehind writeBehind = createAsyncWriteBehind();
    writeBehind.write(new Element("", ""));
  }

  @Test(expected = IllegalStateException.class)
  public void testWriteThrowsWhenNotStopped() {
    AsyncWriteBehind writeBehind = createAsyncWriteBehind();
    writeBehind.start(mock(CacheWriter.class));
    writeBehind.stop();
    writeBehind.write(new Element("", ""));
  }

  @Test
  public void testWorksWhenStarted() {
    AsyncWriteBehind writeBehind = createAsyncWriteBehind();
    writeBehind.start(mock(CacheWriter.class));
    writeBehind.write(new Element("", ""));
    writeBehind.delete(new CacheEntry("", new Element("", "")));
    writeBehind.stop();
  }

  private AsyncWriteBehind createAsyncWriteBehind() {
    final Ehcache mockCache = mock(Ehcache.class);
    final CacheConfiguration mockCacheConfig = mock(CacheConfiguration.class);
    final TerracottaConfiguration mockTcConfig = mock(TerracottaConfiguration.class);
    when(mockCacheConfig.getTerracottaConfiguration()).thenReturn(mockTcConfig);
    when(mockCache.getCacheConfiguration()).thenReturn(mockCacheConfig);
    final CacheWriterConfiguration mockCacheWriterConfig = mock(CacheWriterConfiguration.class);
    when(mockCacheConfig.getCacheWriterConfiguration()).thenReturn(mockCacheWriterConfig);
    return new AsyncWriteBehind(mock(AsyncCoordinator.class), mockCache);
  }
}

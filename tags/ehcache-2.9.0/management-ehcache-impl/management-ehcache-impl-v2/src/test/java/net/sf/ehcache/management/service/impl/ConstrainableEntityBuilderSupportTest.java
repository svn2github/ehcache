package net.sf.ehcache.management.service.impl;

import java.util.HashSet;
import java.util.Set;

import net.sf.ehcache.management.resource.CacheEntityV2;
import net.sf.ehcache.management.sampled.CacheSampler;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.TimeStampedCounterValue;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class ConstrainableEntityBuilderSupportTest {

  @Test
  public void smallSizesAreCorrect() throws Exception {
    CacheSampler sampler = mock(CacheSampler.class);
    when(sampler.getSize()).thenReturn(1001L);

    SampledCounter sizeSampleCounter = mock(SampledCounter.class);
    when(sizeSampleCounter.getMostRecentSample()).thenReturn(new TimeStampedCounterValue(0, 1002L));
    SampledCounter remoteSizeSampleCounter = mock(SampledCounter.class);
    when(remoteSizeSampleCounter.getMostRecentSample()).thenReturn(new TimeStampedCounterValue(0, 1003L));
    when(sampler.getSizeSample()).thenReturn(sizeSampleCounter);
    when(sampler.getRemoteSizeSample()).thenReturn(remoteSizeSampleCounter);
    
    Set<String> constraintAttributes =  new HashSet<String>();
    constraintAttributes.add("Size");
    constraintAttributes.add("SizeSample");
    constraintAttributes.add("RemoteSizeSample");
    
	CacheEntityV2 cacheEntity = CacheEntityBuilderV2.createWith(sampler, "cache").add(constraintAttributes).build().iterator().next();

    assertThat((Long)cacheEntity.getAttributes().get("Size"), is(1001L));
    assertThat((Long)cacheEntity.getAttributes().get("SizeSample"), is(1002L));
    assertThat((Long)cacheEntity.getAttributes().get("RemoteSizeSample"), is(1003L));
  }

  @Test
  public void unsignedIntSizesCoalescedToLong() throws Exception {
    CacheSampler sampler = mock(CacheSampler.class);
    when(sampler.getSize()).thenReturn(Integer.MAX_VALUE + 1L);

    SampledCounter sizeSampleCounter = mock(SampledCounter.class);
    when(sizeSampleCounter.getMostRecentSample()).thenReturn(new TimeStampedCounterValue(0, Integer.MAX_VALUE + 2L));
    SampledCounter remoteSizeSampleCounter = mock(SampledCounter.class);
    when(remoteSizeSampleCounter.getMostRecentSample()).thenReturn(new TimeStampedCounterValue(0, Integer.MAX_VALUE + 3L));
    when(sampler.getSizeSample()).thenReturn(sizeSampleCounter);
    when(sampler.getRemoteSizeSample()).thenReturn(remoteSizeSampleCounter);

    Set<String> constraintAttributes =  new HashSet<String>();
    constraintAttributes.add("Size");
    constraintAttributes.add("SizeSample");
    constraintAttributes.add("RemoteSizeSample");
    
	CacheEntityV2 cacheEntity = CacheEntityBuilderV2.createWith(sampler, "cache").add(constraintAttributes).build().iterator().next();

    assertThat((Long)cacheEntity.getAttributes().get("Size"), is(2147483648L));
    assertThat((Long)cacheEntity.getAttributes().get("SizeSample"), is(2147483649L));
    assertThat((Long)cacheEntity.getAttributes().get("RemoteSizeSample"), is(2147483650L));
  }
}

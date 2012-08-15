/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

public interface CacheCoherence {

  public static final String LOGGING_ENABLED_PROPERTY                      = "ehcache.incoherent.logging";
  public static final String LOCAL_BUFFER_PUTS_BATCH_SIZE_PROPERTY         = "ehcache.incoherent.putsBatchSize";
  public static final String LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE_PROPERTY    = "ehcache.incoherent.putsBatchByteSize";
  public static final String LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS_PROPERTY  = "ehcache.incoherent.putsBatchTimeInMillis";
  public static final String LOCAL_BUFFER_PUTS_THROTTLE_SIZE_PROPERTY      = "ehcache.incoherent.throttlePutsAtSize";
  public static final String LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE_PROPERTY = "ehcache.incoherent.throttlePutByteSize";

  public void loadReferences();

  public void acquireReadLock();

  public void acquireWriteLock();

  public void releaseReadLock();

  public void releaseWriteLock();

  public void waitUntilClusterCoherent() throws InterruptedException;

  public boolean isClusterCoherent();

  public boolean isNodeCoherent();

  public void setNodeCoherent(boolean coherent);

  public boolean isClusterOnline();

  public void dispose();
}

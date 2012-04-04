/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.cache;

/**
 * This is a mirror of the core type com.tc.cache.ExpirableEntry
 * 
 * @author teck
 */
public interface ExpirableEntry {

  int expiresAt(final int tti, final int ttl);

}

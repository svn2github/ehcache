/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package org.terracotta.modules.ehcache.wan;

/**
 * @author Eugene Shelestovich
 */
public interface Watchable {

  /**
   *
   */
  void goLive();

  void die();

  /**
   * Checks whether or not a given Watchable is alive.
   *
   * @return {@code true} if a given Watchable is alive, {@code false} otherwise
   */
  boolean probeLiveness();

  /**
   * Returns a name which uniquely identifies the Watchable.
   *
   * @return unique name
   */
  String name();
}

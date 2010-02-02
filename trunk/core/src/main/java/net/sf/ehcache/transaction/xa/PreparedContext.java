/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.transaction.xa;

import java.util.List;
import java.util.Set;

public interface PreparedContext {

  /**
   * Add prepared command
   * @param command
   */
  public abstract void addCommand(VersionAwareCommand command);

  /**
   * Return list of prepared commands
   * @return
   */
  public abstract List<VersionAwareCommand> getCommands();

  /**
   * return set of keys associated with prepared commands
   * @return
   */
  public Set<Object> getUpdatedKeys();
}
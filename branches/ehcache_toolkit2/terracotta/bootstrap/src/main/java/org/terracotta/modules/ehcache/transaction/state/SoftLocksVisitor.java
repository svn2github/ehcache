/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction.state;

import org.terracotta.modules.ehcache.transaction.SoftLockId;

public interface SoftLocksVisitor {

  void visitSoftLock(SoftLockId softLockId);

}

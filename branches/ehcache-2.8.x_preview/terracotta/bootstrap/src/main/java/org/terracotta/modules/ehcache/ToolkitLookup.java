/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import org.terracotta.toolkit.Toolkit;

/**
 * Package protected interface used in tests to lookup internal toolkit.
 */
interface ToolkitLookup {
  Toolkit getToolkit();
}

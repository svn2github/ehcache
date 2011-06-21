/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.pool.sizeof;

import net.sf.ehcache.pool.sizeof.SizeOfAgent;
import net.sf.ehcache.pool.sizeof.filter.PassThroughFilter;
import net.sf.ehcache.pool.sizeof.filter.SizeOfFilter;


import static net.sf.ehcache.pool.sizeof.JvmInformation.MINIMUM_OBJECT_SIZE;

public class AgentSizeOf extends SizeOf {

  private static final boolean AGENT_LOADED = SizeOfAgent.isAvailable() || AgentLoader.loadAgent();

  public AgentSizeOf() throws UnsupportedOperationException {
    this(new PassThroughFilter());
  }

  public AgentSizeOf(SizeOfFilter filter) throws UnsupportedOperationException {
    this(filter, true);
  }

  public AgentSizeOf(SizeOfFilter filter, boolean caching) throws UnsupportedOperationException {
    super(filter, caching);
    if (!AGENT_LOADED) {
      throw new UnsupportedOperationException("Agent not available or loadable");
    }
  }

  @Override
  protected long measureSizeOf(Object obj) {
    return Math.max(MINIMUM_OBJECT_SIZE, AgentLoader.agentSizeOf(obj));
  }
}

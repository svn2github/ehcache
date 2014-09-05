/**
 *  Copyright Terracotta, Inc.
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
package org.terracotta.modules.ehcache.management;

import net.sf.ehcache.management.event.ManagementEventSink;
import org.terracotta.toolkit.internal.feature.ManagementInternalFeature;
import org.terracotta.toolkit.internal.feature.ToolkitManagementEvent;

import java.io.Serializable;

/**
 * @author Ludovic Orban
 */
public class ClusteredManagementEventSink implements ManagementEventSink {
  private final ManagementInternalFeature feature;

  public ClusteredManagementEventSink(ManagementInternalFeature feature) {
    this.feature = feature;
  }

  public void sendManagementEvent(Serializable event, String type) {
    feature.sendEvent(new ToolkitManagementEvent(event, type));
  }

}

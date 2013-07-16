/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package org.terracotta.modules.ehcache.store;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;

import org.junit.Test;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;

/**
 * ToolkitNonStopConfigurationTest
 */
public class ToolkitNonStopConfigurationTest {

  @Test
  public void testTimeoutBehaviorTypeConversion() {
    TimeoutBehaviorConfiguration timeoutBehaviorException = new TimeoutBehaviorConfiguration();
    timeoutBehaviorException.setType("exception");
    assertConfig(timeoutBehaviorException, NonStopConfigurationFields.NonStopReadTimeoutBehavior.EXCEPTION,
            NonStopConfigurationFields.NonStopWriteTimeoutBehavior.EXCEPTION);

    timeoutBehaviorException.setType("noop");
    assertConfig(timeoutBehaviorException, NonStopConfigurationFields.NonStopReadTimeoutBehavior.NO_OP, NonStopConfigurationFields.NonStopWriteTimeoutBehavior.NO_OP);

    timeoutBehaviorException.setType("localReads");
    assertConfig(timeoutBehaviorException, NonStopConfigurationFields.NonStopReadTimeoutBehavior.LOCAL_READS, NonStopConfigurationFields.NonStopWriteTimeoutBehavior.NO_OP);

    timeoutBehaviorException.setType("localReadsAndExceptionOnWrite");
    assertConfig(timeoutBehaviorException, NonStopConfigurationFields.NonStopReadTimeoutBehavior.LOCAL_READS, NonStopConfigurationFields.NonStopWriteTimeoutBehavior.EXCEPTION);
  }

  private void assertConfig(TimeoutBehaviorConfiguration timeoutBehaviorConfiguration,
                            NonStopConfigurationFields.NonStopReadTimeoutBehavior readTimeoutBehavior,
                            NonStopConfigurationFields.NonStopWriteTimeoutBehavior writeTimeoutBehavior) {
    NonstopConfiguration nonstopConfiguration = new NonstopConfiguration();
    ToolkitNonStopConfiguration nonStopConfiguration = new ToolkitNonStopConfiguration(nonstopConfiguration.timeoutBehavior(timeoutBehaviorConfiguration));
    assertThat(nonStopConfiguration.getReadOpNonStopTimeoutBehavior(), is(readTimeoutBehavior));
    assertThat(nonStopConfiguration.getWriteOpNonStopTimeoutBehavior(), is(writeTimeoutBehavior));
  }
}

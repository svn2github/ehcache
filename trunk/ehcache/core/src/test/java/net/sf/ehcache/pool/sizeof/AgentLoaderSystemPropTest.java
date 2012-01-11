package net.sf.ehcache.pool.sizeof;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class AgentLoaderSystemPropTest {

    @Test
    public void testLoadsAgentIntoSystemPropsWhenRequired() {
        System.setProperty("net.sf.ehcache.sizeof.agent.instrumentationSystemProperty", "true");
        AgentLoader.loadAgent();
        if(AgentLoader.agentIsAvailable()) {
            assertThat(System.getProperties().get("net.sf.ehcache.sizeof.agent.instrumentation"), notNullValue());
        }
    }
}

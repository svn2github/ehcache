package net.sf.ehcache.management.event;

import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.TerracottaClient;
import org.junit.Test;

import java.io.Serializable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class DelegatingManagementEventSinkTest {

    @Test
    public void testNullClusteredInstanceFactoryForUnclusteredMode() throws Exception {
        TerracottaClient terracottaClient = mock(TerracottaClient.class);
        DelegatingManagementEventSink delegatingManagementEventSink = new DelegatingManagementEventSink(terracottaClient);
        delegatingManagementEventSink.sendManagementEvent("event1", "type1");
    }

    @Test
    public void testClusteredInstanceFactoryChangeDetected() throws Exception {
        TerracottaClient terracottaClient = mock(TerracottaClient.class);
        ClusteredInstanceFactory clusteredInstanceFactory_1 = mock(ClusteredInstanceFactory.class);
        ClusteredInstanceFactory clusteredInstanceFactory_2 = mock(ClusteredInstanceFactory.class);
        ManagementEventSink managementEventSink_1 = mock(ManagementEventSink.class);
        ManagementEventSink managementEventSink_2 = mock(ManagementEventSink.class);
        when(clusteredInstanceFactory_1.createEventSink()).thenReturn(managementEventSink_1);
        when(clusteredInstanceFactory_2.createEventSink()).thenReturn(managementEventSink_2);
        when(terracottaClient.getClusteredInstanceFactory()).thenReturn(clusteredInstanceFactory_1);


        DelegatingManagementEventSink delegatingManagementEventSink = new DelegatingManagementEventSink(terracottaClient);
        delegatingManagementEventSink.sendManagementEvent("event1", "type1");
        verify(managementEventSink_1, times(1)).sendManagementEvent("event1", "type1");
        verify(managementEventSink_2, times(0)).sendManagementEvent(any(Serializable.class), anyString());

        // switch clusteredInstanceFactory_2
        when(terracottaClient.getClusteredInstanceFactory()).thenReturn(clusteredInstanceFactory_2);
        delegatingManagementEventSink.sendManagementEvent("event2", "type2");
        delegatingManagementEventSink.sendManagementEvent("event2", "type2");

        verify(managementEventSink_1, times(1)).sendManagementEvent("event1", "type1");
        verify(managementEventSink_2, times(2)).sendManagementEvent("event2", "type2");
    }
}

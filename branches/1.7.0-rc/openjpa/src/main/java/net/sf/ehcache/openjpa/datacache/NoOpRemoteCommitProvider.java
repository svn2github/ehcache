package net.sf.ehcache.openjpa.datacache;

import org.apache.openjpa.event.AbstractRemoteCommitProvider;
import org.apache.openjpa.event.RemoteCommitEvent;
import org.apache.openjpa.event.RemoteCommitProvider;

public class NoOpRemoteCommitProvider extends AbstractRemoteCommitProvider
        implements RemoteCommitProvider {

    public void broadcast(RemoteCommitEvent event) {
    }

    public void close() {
    }

}

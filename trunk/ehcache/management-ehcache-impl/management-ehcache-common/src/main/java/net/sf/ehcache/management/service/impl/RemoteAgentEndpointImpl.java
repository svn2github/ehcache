package net.sf.ehcache.management.service.impl;

import org.terracotta.management.l1bridge.AbstractRemoteAgentEndpointImpl;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.l1bridge.RemoteCallException;

public class RemoteAgentEndpointImpl extends AbstractRemoteAgentEndpointImpl {
  public static final String AGENCY = "Ehcache";

  private final ThreadLocal<Boolean> tsaBridged = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };

  protected boolean isTsaSecured() {
    return false;
  }

  public boolean isTsaBridged() {
    return tsaBridged.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] invoke(RemoteCallDescriptor remoteCallDescriptor) throws RemoteCallException {
    try {
      tsaBridged.set(true);
      return super.invoke(remoteCallDescriptor);
    } finally {
      tsaBridged.set(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getVersion() {
    return this.getClass().getPackage().getImplementationVersion();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAgency() {
    return AGENCY;
  }

}

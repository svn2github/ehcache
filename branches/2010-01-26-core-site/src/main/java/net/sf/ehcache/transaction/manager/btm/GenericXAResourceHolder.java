package net.sf.ehcache.transaction.manager.btm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.transaction.xa.XAResource;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;

public class GenericXAResourceHolder extends AbstractXAResourceHolder {

	private final XAResource resource;
	private final ResourceBean bean;

	public GenericXAResourceHolder(XAResource resource, ResourceBean bean) {
		this.resource = resource;
		this.bean = bean;
	}

	public RecoveryXAResourceHolder createRecoveryXAResourceHolder() {
		return new RecoveryXAResourceHolder(this);
	}

	public XAResource getXAResource() {
		return resource;
	}
	
	public XAResourceHolderState getXAResourceHolderState() {
		return new XAResourceHolderState(this, bean);
	}

	public void close() throws Exception {
		throw new UnsupportedOperationException(
				"GenericXAResourceHolder cannot be used with an XAPool implementation");
	}

	public Object getConnectionHandle() throws Exception {
		throw new UnsupportedOperationException(
				"GenericXAResourceHolder cannot be used with an XAPool implementation");
	}

	public Date getLastReleaseDate() {
		throw new UnsupportedOperationException(
				"GenericXAResourceHolder cannot be used with an XAPool implementation");
	}

	public List getXAResourceHolders() {
		List xaResourceHolders = new ArrayList();
		xaResourceHolders.add(this);
		return xaResourceHolders;
	}

}

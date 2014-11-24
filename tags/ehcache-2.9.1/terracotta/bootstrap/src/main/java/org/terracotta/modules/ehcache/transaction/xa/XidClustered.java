/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction.xa;

import java.io.Serializable;
import java.util.Arrays;

import javax.transaction.xa.Xid;

/**
 * @author Alex Snaps
 */
public final class XidClustered implements Xid, Serializable {

  private final int formatId;
  private final byte[] globalTxId;
  private final byte[] branchQualifier;

  public XidClustered(Xid xid) {
    this.formatId = xid.getFormatId();
    this.globalTxId = xid.getGlobalTransactionId();
    this.branchQualifier = xid.getBranchQualifier();
  }
  
  public XidClustered(int formatId, byte [] globalTxId, byte [] branchQualifier) {
    this.formatId = formatId;
    this.globalTxId = globalTxId;
    this.branchQualifier = branchQualifier;
  }

  @Override
  public int getFormatId() {
    return formatId;
  }

  @Override
  public byte[] getGlobalTransactionId() {
    return globalTxId;
  }

  @Override
  public byte[] getBranchQualifier() {
    return branchQualifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    XidClustered that = (XidClustered)o;

    return formatId == that.formatId && Arrays.equals(branchQualifier, that.branchQualifier) && Arrays
      .equals(globalTxId, that.globalTxId);

  }

  @Override
  public int hashCode() {
    int result = formatId;
    result = 31 * result + (globalTxId != null ? Arrays.hashCode(globalTxId) : 0);
    result = 31 * result + (branchQualifier != null ? Arrays.hashCode(branchQualifier) : 0);
    return result;
  }

  @Override
  public String toString() {
    return "XidClustered{" +
           "formatId=" + formatId +
           ", globalTxId=" + Arrays.toString(globalTxId) +
           ", branchQualifier=" + Arrays.toString(branchQualifier) +
           '}';
  }
}

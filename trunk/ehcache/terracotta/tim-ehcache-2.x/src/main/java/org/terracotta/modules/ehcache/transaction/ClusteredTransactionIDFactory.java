/**
 *  Copyright 2003-2009 Terracotta, Inc.
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
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.xa.XidTransactionID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.transaction.xa.ClusteredXidTransactionID;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public class ClusteredTransactionIDFactory implements TransactionIDFactory {

  private static final Logger LOG = LoggerFactory.getLogger(ClusteredTransactionIDFactory.class.getName());


  private final String clusterUUID;

  public ClusteredTransactionIDFactory(String clusterUUID) {
    this.clusterUUID = clusterUUID;
    LOG.debug("ClusteredTransactionIDFactory UUID: {}", clusterUUID);
  }

  public TransactionID createTransactionID() {
    return new ClusteredTransactionID(clusterUUID);
  }

  public XidTransactionID createXidTransactionID(Xid xid) {
    return new ClusteredXidTransactionID(xid);
  }
}

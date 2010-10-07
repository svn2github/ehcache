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

package net.sf.ehcache.transaction.xa;

import javax.transaction.xa.XAException;

/**
 * Class to delegate XAResource classes to internal thread
 * 
 * @author Nabib El-Rahman
 *
 */
public interface XARequestProcessor {
    
  
    /**
     * Process the XAResource method to another thread.
     * 
     * @param request the XARequest to be processed
     * @return the potential XA flags for the XAResource to return to the TransactionManager
     * @throws XAException Depending on the XARequest type being executed, some XAException can happen
     */
    public int process(XARequest request) throws XAException;
    
    
    
}

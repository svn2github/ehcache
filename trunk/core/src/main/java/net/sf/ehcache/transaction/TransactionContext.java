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

package net.sf.ehcache.transaction;

import java.util.Collection;
import java.util.List;

import javax.transaction.Transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.xa.VersionAwareCommand;

/**
 * @author Alex Snaps
 */
public interface TransactionContext {

    void addCommand(Command command, Element element);
    
    Transaction getTransaction();
    
    public Element get(Object key);

    boolean isRemoved(Object key);

    Collection getAddedKeys();

    Collection getRemovedKeys();

    int getSizeModifier();
    
    List<VersionAwareCommand> getCommands();
}

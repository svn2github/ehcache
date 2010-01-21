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

import java.io.Serializable;

import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.Command;
import net.sf.ehcache.transaction.StoreWriteCommand;

public class VersionAwareWrapper implements Command, VersionAwareCommand {
    
    private final Command command;
    private final long version;
    private final Serializable key;
    
    public VersionAwareWrapper(Command command) {
        this.command = command;
        this.version = -1;
        this.key = null;
    }
    
    public VersionAwareWrapper(Command command,  long version, Serializable key) {
        this.command = command;
        this.version = version;
        this.key = key;
    }
    
    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.VersionAwareCommand#isWriteCommand()
     */
    public boolean isWriteCommand() {
        return (command instanceof StoreWriteCommand);
    }
    
    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.VersionAwareCommand#execute(net.sf.ehcache.store.Store)
     */
    public boolean execute(Store store) {
        return command.execute(store);
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.VersionAwareCommand#isPut(java.lang.Object)
     */
    public boolean isPut(Object key) {
        return command.isPut(key);
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.VersionAwareCommand#isRemove(java.lang.Object)
     */
    public boolean isRemove(Object key) {
        return command.isRemove(key);
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.VersionAwareCommand#isVersionAware()
     */
    public boolean isVersionAware() {
        return key != null;
    }
    
    /* (non-Javadoc)
     * @see net.sf.ehcache.transaction.xa.VersionAwareCommand#getVersion()
     */
    public long getVersion() {
        return version;
    }
  
    public Serializable getKey() {
        return key;
    }

    public String getCommandName() {
        return command.getCommandName();
    }

}

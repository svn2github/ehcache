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

import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.Command;
import net.sf.ehcache.transaction.StoreWriteCommand;

/**
 * @author Nabib El-Rahman
 */
public class VersionAwareWrapper implements Command, VersionAwareCommand {
    
    private final Command command;
    private final long version;
    private final Object key;

    /**
     * Constructor
     * @param command the underlying command
     */
    public VersionAwareWrapper(Command command) {
        this.command = command;
        this.version = -1;
        this.key = null;
    }

    /**
     * Constructor
     * @param command the underlying command
     * @param version the version
     * @param key the key
     */
    public VersionAwareWrapper(Command command,  long version, Object key) {
        this.command = command;
        this.version = version;
        this.key = key;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isWriteCommand() {
        return (command instanceof StoreWriteCommand);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean execute(Store store) {
        return command.execute(store);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPut(Object key) {
        return command.isPut(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRemove(Object key) {
        return command.isRemove(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isVersionAware() {
        return key != null;
    }
    
    /**
     * {@inheritDoc}
     */
    public long getVersion() {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    public Object getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    public String getCommandName() {
        return command.getCommandName();
    }

}

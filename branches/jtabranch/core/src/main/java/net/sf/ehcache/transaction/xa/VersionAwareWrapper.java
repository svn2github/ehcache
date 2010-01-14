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

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.Command;
import net.sf.ehcache.transaction.StoreWriteCommand;

public class VersionAwareWrapper implements Command {
    
    private final Command command;
    private final long version;
    private final Element element;
    
    public VersionAwareWrapper(Command command) {
        this.command = command;
        this.version = -1;
        this.element = null;
    }
    
    public VersionAwareWrapper(Command command,  long version, Element element) {
        this.command = command;
        this.version = version;
        this.element = element;
    }
    
    public boolean isWriteCommand() {
        return (command instanceof StoreWriteCommand);
    }
    
    public void execute(Store store) {
        command.execute(store);
    } 
    
    public boolean isVersionAware() {
        return element != null;
    }
    
    public long getVersion() {
        return version;
    }
    
    public Element getElement() {
        return element;
    }
    
    

}

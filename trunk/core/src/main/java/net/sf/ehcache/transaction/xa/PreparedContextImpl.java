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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author nelrahma
 *
 */
public class PreparedContextImpl implements PreparedContext {

    private final          List<PreparedCommand> commands   = new ArrayList<PreparedCommand>();
    private       volatile boolean                   rolledBack;
    private       volatile boolean                   commited;

    /**
     * {@inheritDoc}
     */    
    public void addCommand(VersionAwareCommand command) {
      commands.add(new PreparedCommandImpl(command.getKey(), command.isWriteCommand()));
    }

    /**
     * {@inheritDoc}
     */    
    public List<PreparedCommand> getPreparedCommands() {
      return Collections.unmodifiableList(commands);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Object> getUpdatedKeys() {
        
        Set<Object> keys = new HashSet<Object>();
        for (PreparedCommand command : getPreparedCommands()) {
            Object key = command.getKey();
            if (key != null) {
                keys.add(key);
            }
        }

        return Collections.unmodifiableSet(keys);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRolledBack() {
        return rolledBack;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCommited() {
        return commited;
    }

    /**
     * {@inheritDoc}
     */
    public void setRolledBack(final boolean rolledBack) {
        if (this.commited && rolledBack) {
            throw new IllegalStateException("Context was marked as commited already!");
        }
        this.rolledBack = rolledBack;
    }

    /**
     * {@inheritDoc}
     */
    public void setCommited(final boolean commited) {
        if (this.rolledBack && commited) {
            throw new IllegalStateException("Context was marked as rolled back already!");
        }
        this.commited = commited;
    }
}

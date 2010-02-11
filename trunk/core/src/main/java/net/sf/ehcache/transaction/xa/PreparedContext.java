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

import java.util.List;
import java.util.Set;

/**
 * @author nelrahma
 */
public interface PreparedContext {

    /**
     * Add prepared command
     *
     * @param command the command to be added to the queue
     */
    public abstract void addCommand(VersionAwareCommand command);

    /**
     * Return list of prepared commands
     *
     * @return all commands from the queue
     */
    public abstract List<PreparedCommand> getPreparedCommands();

    /**
     * return set of keys associated with prepared commands
     *
     * @return a set of keys to be updated (put or remove)
     */
    public Set<Object> getUpdatedKeys();


    /**
     * Checks whether the transaction branch associated with this context was rolled back internally
     * @return true, if transaction was roleld back internally
     */
    boolean isRolledBack();

    /**
     * Checks whether the transaction branch associated with this context was commited internally
     * @return true, if transaction was commited internally
     */
    boolean isCommited();

    /**
     * Marks the transaction branch associated with this context as rolled back
     * @param rolledBack true, if was rolled back, false otherwise
     */
    void setRolledBack(boolean rolledBack);

    /**
     * Marks the transaction branch associated with this context as commited
     * @param commited true, if was commited, false otherwise
     */
    void setCommited(boolean commited);
}
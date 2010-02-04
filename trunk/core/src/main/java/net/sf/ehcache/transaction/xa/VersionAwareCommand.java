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

import net.sf.ehcache.transaction.Command;

/**
 * A more specialized type of Command, that is potentially aware of a version of the Element involved in its execution against the Store.
 * @author Alex Snaps
 */
public interface VersionAwareCommand extends Command {

    /**
     * Checks whether this command is a write command to the underlying store
     * @return true if the command would mutate the store on {@link Command#execute(net.sf.ehcache.store.Store)}
     */
    boolean isWriteCommand();

    /**
     * Checks whether this command aware of any version scheme of the Element if affects
     * @return true if so
     */
    boolean isVersionAware();

    /**
     * getter to the version of the Element this affects
     * @return version number
     */
    long getVersion();

    /**
     * Getter to the key of the Element this command affects
     * @return key
     */
    public Object getKey();

}
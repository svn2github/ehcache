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

/**
 * This class store relevant information about a command, once it has been prepared
 * @author Nabib El-Rahman
 *
 */
public interface PreparedCommand {

    /**
     * Key of command
     * @return the key involved with that command
     */
    Object getKey();

    /**
     * If this is write command (i.e. put, remove)
     * @return true, if executing that command will mutate the underlying store. Otherwise false
     */
    boolean isWriteCommand();

}

/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.distribution.EventMessage;

/**
 * Actions that tie in with EventMessage actions. EventMessage has not been updated to use enums.
 *
 * @author Greg Luck
 */
public enum Action {

    /**
     *
     */
    PUT(EventMessage.PUT),

    /**
     *
     */
    REMOVE(EventMessage.REMOVE),

    /**
     *
     */
    REMOVE_ALL(EventMessage.REMOVE_ALL),

    /**
     *
     */
    GET(10);

    private int action;

    /**
     * @param mode
     */
    Action(int mode) {
        this.action = mode;
    }

    /**
     * @param value
     * @return The action enum corresponsing to the string value
     */
    public static Action forString(String value) {
        for (Action action : values()) {
            if (action.name().equals(value)) {
                return action;
            }
        }
        return null;
    }


    /**
     * @return an int value for the action. The same int values as EventMessage are used
     */
    public int toInt() {
        return action;
    }
}

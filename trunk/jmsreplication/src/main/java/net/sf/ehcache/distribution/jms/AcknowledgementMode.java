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

import javax.jms.Session;

/**
 * An enum of JMS acknowledgement mode.
 *
 * @author Greg Luck
 */
public enum AcknowledgementMode {

    /**
     *
     */
    AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE),

    //CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE), not supported
    /**
     *
     */
    DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE),

    /**
     *
     */
    SESSION_TRANSACTED(Session.SESSION_TRANSACTED);

    private int mode;

    /**
     * Constructor
     *
     * @param mode an integer being the mode as per JMS
     */
    AcknowledgementMode(int mode) {
        this.mode = mode;
    }

    /**
     * Return an AcknowledgementMode that matches a string
     *
     * @param value matched with the name() value of the enum
     * @return the matching enum, or DUPS_OK_ACKNOWLEDGE if there is no match
     */
    public static AcknowledgementMode forString(String value) {
        for (AcknowledgementMode acknowledgementMode : values()) {
            if (acknowledgementMode.name().equals(value)) {
                return acknowledgementMode;
            }
        }
        return DUPS_OK_ACKNOWLEDGE;
    }

    /**
     * Returns the int value of the enum.
     *
     * @return an int matching the JMS values
     */
    public int toInt() {
        return mode;
    }
}
